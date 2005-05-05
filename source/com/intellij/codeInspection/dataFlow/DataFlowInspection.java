/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: Dec 24, 2001
 * Time: 2:46:32 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.intellij.codeInspection.dataFlow;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.dataFlow.instructions.*;
import com.intellij.codeInspection.ex.AddAssertStatementFix;
import com.intellij.codeInspection.ex.BaseLocalInspectionTool;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import com.intellij.codeInsight.daemon.impl.quickfix.SimplifyBooleanExpressionFix;

import java.util.*;

public class DataFlowInspection extends BaseLocalInspectionTool {
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeInspection.dataFlow.DataFlowInspection");

  private static final String DISPLAY_NAME = "Constant conditions & exceptions";
  public static final String SHORT_NAME = "ConstantConditions";

  public DataFlowInspection() {
  }

  public ProblemDescriptor[] checkMethod(PsiMethod method, InspectionManager manager, boolean isOnTheFly) {
    return analyzeCodeBlock(method.getBody(), manager);
  }

  public ProblemDescriptor[] checkClass(PsiClass aClass, InspectionManager manager, boolean isOnTheFly) {
    List<ProblemDescriptor> allProblems = null;
    final PsiClassInitializer[] initializers = aClass.getInitializers();
    for (PsiClassInitializer initializer : initializers) {
      final ProblemDescriptor[] problems = analyzeCodeBlock(initializer.getBody(), manager);
      if (problems != null) {
        if (allProblems == null) {
          allProblems = new ArrayList<ProblemDescriptor>(1);
        }
        allProblems.addAll(Arrays.asList(problems));
      }
    }
    return allProblems == null ? null : allProblems.toArray(new ProblemDescriptor[allProblems.size()]);
  }

  private static ProblemDescriptor[] analyzeCodeBlock(final PsiCodeBlock body, InspectionManager manager) {
    if (body == null) return null;
    DataFlowRunner dfaRunner = new DataFlowRunner();
    if (dfaRunner.analyzeMethod(body)) {
      HashSet[] constConditions = dfaRunner.getConstConditionalExpressions();
      if (constConditions[0].size() > 0 ||
          constConditions[1].size() > 0 ||
          dfaRunner.getNPEInstructions().size() > 0 ||
          dfaRunner.getCCEInstructions().size() > 0 ||
          dfaRunner.getRedundantInstanceofs().size() > 0 ||
          dfaRunner.getNullableArguments().size() > 0 ||
          dfaRunner.getNullableAssignments().size() > 0
        ) {
        return createDescription(dfaRunner, manager);
      }
    }

    return null;
  }

  private static LocalQuickFix createAssertNotNullFix(PsiExpression qualifier) {
    if (qualifier != null && qualifier.getManager().getEffectiveLanguageLevel().hasAssertKeyword() &&
        !(qualifier instanceof PsiMethodCallExpression)) {
      try {
        PsiBinaryExpression binary = (PsiBinaryExpression)qualifier.getManager().getElementFactory().createExpressionFromText("a != null",
                                                                                                                              null);
        binary.getLOperand().replace(qualifier);
        return new AddAssertStatementFix(binary);
      }
      catch (IncorrectOperationException e) {
        LOG.error(e);
        return null;
      }
    }
    return null;
  }

  private static ProblemDescriptor[] createDescription(DataFlowRunner runner, InspectionManager manager) {
    HashSet<Instruction>[] constConditions = runner.getConstConditionalExpressions();
    HashSet<Instruction> trueSet = constConditions[0];
    HashSet<Instruction> falseSet = constConditions[1];
    Set<Instruction> npeSet = runner.getNPEInstructions();
    Set<Instruction> cceSet = runner.getCCEInstructions();
    Set<Instruction> redundantInstanceofs = runner.getRedundantInstanceofs();

    ArrayList<Instruction> allProblems = new ArrayList<Instruction>();
    for (Instruction instr : trueSet) {
      allProblems.add((Instruction)instr);
    }

    for (Instruction instr : falseSet) {
      allProblems.add(instr);
    }

    for (Instruction methodCallInstruction : npeSet) {
      allProblems.add(methodCallInstruction);
    }

    for (Instruction typeCastInstruction : cceSet) {
      allProblems.add(typeCastInstruction);
    }

    for (Instruction instruction : redundantInstanceofs) {
      allProblems.add(instruction);
    }

    Collections.sort(allProblems, new Comparator() {
      public int compare(Object o1, Object o2) {
        int i1 = ((Instruction)o1).getIndex();
        int i2 = ((Instruction)o2).getIndex();

        if (i1 == i2) return 0;
        if (i1 > i2) return 1;

        return -1;
      }
    });

    ArrayList<ProblemDescriptor> descriptions = new ArrayList<ProblemDescriptor>(allProblems.size());
    HashSet<PsiElement> reportedAnchors = new HashSet<PsiElement>();

    for (int i = 0; i < allProblems.size(); i++) {
      Instruction instruction = allProblems.get(i);

      if (instruction instanceof MethodCallInstruction) {
        MethodCallInstruction mcInstruction = (MethodCallInstruction)instruction;
        PsiMethodCallExpression callExpression = mcInstruction.getCallExpression();
        LocalQuickFix fix = createAssertNotNullFix(callExpression.getMethodExpression().getQualifierExpression());

        descriptions.add(manager.createProblemDescriptor(mcInstruction.getCallExpression(),
                                                         "Method invocation <code>#ref</code> #loc may produce <code>java.lang.NullPointerException</code>.",
                                                         fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
      }
      else if (instruction instanceof FieldReferenceInstruction) {
        FieldReferenceInstruction frInstruction = (FieldReferenceInstruction)instruction;
        PsiExpression expression = frInstruction.getExpression();
        if (expression instanceof PsiArrayAccessExpression) {
          LocalQuickFix fix = createAssertNotNullFix(((PsiArrayAccessExpression)expression).getArrayExpression());
          descriptions.add(manager.createProblemDescriptor(expression,
                                                           "Array access <code>#ref</code> #loc may produce <code>java.lang.NullPointerException<./code>.",
                                                           fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
        }
        else {
          LocalQuickFix fix = createAssertNotNullFix(((PsiReferenceExpression)expression).getQualifierExpression());
          descriptions.add(manager.createProblemDescriptor(expression,
                                                           "Member variable access <code>#ref</code> #loc may produce <code>java.lang.NullPointerException<./code>.",
                                                           fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
        }
      }
      else if (instruction instanceof TypeCastInstruction) {
        TypeCastInstruction tcInstruction = (TypeCastInstruction)instruction;
        PsiTypeCastExpression typeCast = tcInstruction.getCastExpression();
        descriptions.add(manager.createProblemDescriptor(typeCast.getCastType(),
                                                         "Casting <code>" + typeCast.getOperand().getText() +
                                                         "</code> to <code>#ref</code> #loc may produce <code>java.lang.ClassCastException</code>.",
                                                         null, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
      }
      else if (instruction instanceof BranchingInstruction) {
        PsiElement psiAnchor = ((BranchingInstruction)instruction).getPsiAnchor();
        if (instruction instanceof BinopInstruction && ((BinopInstruction)instruction).isInstanceofRedundant()) {
          if (((BinopInstruction)instruction).canBeNull()) {
            descriptions.add(manager.createProblemDescriptor(psiAnchor,
                                                             "Condition <code>#ref</code> #loc is redundant and can be replaced with <code>!= null</code>",
                                                             new RedundantInstanceofFix(),
                                                             ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
          }
          else {
            final LocalQuickFix localQuickFix = createSimplifyBooleanExpressionFix(psiAnchor, true);
            descriptions.add(manager.createProblemDescriptor(psiAnchor,
                                                             "Condition <code>#ref</code> #loc is always true</code>",
                                                             localQuickFix,
                                                             ProblemHighlightType.GENERIC_ERROR_OR_WARNING));

          }
        }
        else if (psiAnchor instanceof PsiSwitchLabelStatement) {
          if (falseSet.contains(instruction)) {
            descriptions.add(manager.createProblemDescriptor(psiAnchor, "Switch label<code>#ref</code> #loc is unreachable.", null,
                                                             ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
          }
        }
        else if (psiAnchor != null) {
          if (!reportedAnchors.contains(psiAnchor)) {
            final LocalQuickFix localQuickFix = createSimplifyBooleanExpressionFix(psiAnchor, trueSet.contains(instruction));
            descriptions.add(manager.createProblemDescriptor(psiAnchor, "Condition <code>#ref</code> #loc is always <code>" +
                                                                        (trueSet.contains(instruction) ? "true" : "false") +
                                                                        "</code>.", localQuickFix,
                                                             ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
            reportedAnchors.add(psiAnchor);
          }
        }
      }
    }

    Set<PsiExpression> exprs = runner.getNullableArguments();
    for (PsiExpression expr : exprs) {
      descriptions.add(manager.createProblemDescriptor(expr, "Argument <code>#ref</code> #loc is probably null", null,
                                                       ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
    }

    exprs = runner.getNullableAssignments();
    for (PsiExpression expr : exprs) {
      descriptions.add(manager.createProblemDescriptor(expr,
                                                       "Expression <code>#ref</code> probably evaluates to null and is being assigned " +
                                                       "to a variable that is annotated with @NotNull",
                                                       null,
                                                       ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
    }

    return descriptions.toArray(new ProblemDescriptor[descriptions.size()]);
  }

  private static LocalQuickFix createSimplifyBooleanExpressionFix(PsiElement element, final boolean value) {
    if (!(element instanceof PsiExpression)) return null;
    final PsiExpression expression = (PsiExpression)element;
    while (element.getParent() instanceof PsiExpression) {
      element = element.getParent();
    }
    final SimplifyBooleanExpressionFix fix = new SimplifyBooleanExpressionFix(expression, Boolean.valueOf(value));
    // simplify intention already active
    if (SimplifyBooleanExpressionFix.canBeSimplified((PsiExpression)element) != null) return null;
    return new LocalQuickFix() {
      public String getName() {
        return fix.getText();
      }

      public void applyFix(Project project, ProblemDescriptor descriptor) {
        final PsiElement psiElement = descriptor.getPsiElement();
        try {
          LOG.assertTrue(psiElement.isValid());
          fix.invoke(project, null, psiElement.getContainingFile());
        }
        catch (IncorrectOperationException e) {
          LOG.error(e);
        }
      }
    };
  }

  private static class RedundantInstanceofFix implements LocalQuickFix {
    public String getName() {
      return "Replace with != null";
    }

    public void applyFix(Project project, ProblemDescriptor descriptor) {
      final PsiElement psiElement = descriptor.getPsiElement();
      if (psiElement instanceof PsiInstanceOfExpression) {
        try {
          final PsiExpression compareToNull = psiElement.getManager().getElementFactory().
            createExpressionFromText(((PsiInstanceOfExpression)psiElement).getOperand().getText() + " != null",
                                     psiElement.getParent());
          psiElement.replace(compareToNull);
        }
        catch (IncorrectOperationException e) {
          LOG.error(e);
        }
      }
    }
  }


  public String getDisplayName() {
    return DISPLAY_NAME;
  }

  public String getGroupDisplayName() {
    return GROUP_LOCAL_CODE_ANALYSIS;
  }

  public String getShortName() {
    return SHORT_NAME;
  }
}

package com.siyeh.ig.verbose;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.IncorrectOperationException;
import com.siyeh.ig.*;

public class UnnecessaryBlockStatementInspection extends StatementInspection {
    private final UnnecessaryBlockFix fix = new UnnecessaryBlockFix();

    public String getDisplayName() {
        return "Unnecessary code block";
    }

    public String getGroupDisplayName() {
        return GroupNames.VERBOSE_GROUP_NAME;
    }

    public String buildErrorString(PsiElement location) {
        return "Braces around this statement are unnecessary #loc";
    }

    public BaseInspectionVisitor createVisitor(InspectionManager inspectionManager, boolean onTheFly) {
        return new UnnecessaryBlockStatementVisitor(this, inspectionManager, onTheFly);
    }

    public InspectionGadgetsFix buildFix(PsiElement location) {
        return fix;
    }

    private static class UnnecessaryBlockFix extends InspectionGadgetsFix {
        public String getName() {
            return "Unwrap block";
        }

        public void applyFix(Project project, ProblemDescriptor descriptor) {
            final PsiElement leftBrace = descriptor.getPsiElement();
            final PsiCodeBlock block = (PsiCodeBlock) leftBrace.getParent();
            final PsiBlockStatement blockStatement = (PsiBlockStatement) block.getParent();
            final PsiElement containingElement = blockStatement.getParent();
            try {
                final PsiManager psiManager = PsiManager.getInstance(project);
                final CodeStyleManager styleManager = psiManager.getCodeStyleManager();

                final PsiElement[] children = block.getChildren();
                for (int i = 1; i < children.length - 1; i++) {
                    final PsiElement child = children[i];
                    final PsiElement childCopy = child.copy();
                    final PsiElement copiedElement = containingElement.addBefore(childCopy, blockStatement);
                    styleManager.reformat(copiedElement);
                }
                blockStatement.delete();
                styleManager.reformat(containingElement);
            } catch (IncorrectOperationException e) {
                final Class aClass = getClass();
                final String className = aClass.getName();
                final Logger logger = Logger.getInstance(className);
                logger.error(e);
            }
        }
    }

    private static class UnnecessaryBlockStatementVisitor extends BaseInspectionVisitor {
        private UnnecessaryBlockStatementVisitor(BaseInspection inspection, InspectionManager inspectionManager, boolean isOnTheFly) {
            super(inspection, inspectionManager, isOnTheFly);
        }

        public void visitBlockStatement(PsiBlockStatement blockStatement) {
            super.visitBlockStatement(blockStatement);
            final PsiElement parent = blockStatement.getParent();
            if (parent == null) {
                return;
            }
            if (!(parent instanceof PsiCodeBlock)) {
                return;
            }
            final PsiCodeBlock codeBlock = blockStatement.getCodeBlock();
            if (codeBlock == null) {
                return;
            }
            final PsiJavaToken brace = codeBlock.getLBrace();
            if (brace == null) {
                return;
            }
            if (containsDeclarations(blockStatement)) {
                return;
            }
            registerError(brace);
        }

        private static boolean containsDeclarations(PsiBlockStatement blockStatement) {
            final PsiCodeBlock codeBlock = blockStatement.getCodeBlock();
            final PsiStatement[] statements = codeBlock.getStatements();
            if (statements == null) {
                return false;
            }
            for (int i = 0; i < statements.length; i++) {
                final PsiStatement statement = statements[i];
                if (statement instanceof PsiDeclarationStatement) {
                    return true;
                }
            }
            return false;
        }
    }
}

package com.siyeh.ig.bugs;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.siyeh.ig.*;
import com.siyeh.ig.ui.SingleCheckboxOptionsPanel;
import com.siyeh.ig.psiutils.ClassUtils;
import com.siyeh.ig.psiutils.ComparisonUtils;
import com.siyeh.ig.psiutils.ParenthesesUtils;
import com.siyeh.ig.psiutils.TypeUtils;

import javax.swing.*;

public class ObjectEqualityInspection extends ExpressionInspection {
    public boolean m_ignoreEnums = false;

    private final EqualityToEqualsFix fix = new EqualityToEqualsFix();

    public String getDisplayName() {
        return "Object comparison using ==, instead of '.equals()'";
    }

    public String getGroupDisplayName() {
        return GroupNames.BUGS_GROUP_NAME;
    }

    public JComponent createOptionsPanel() {
        return new SingleCheckboxOptionsPanel("Ignore == between enumerated types",
                this, "m_ignoreEnums");
    }

    public String buildErrorString(PsiElement location) {
        return "Object values are compared using '#ref', not '.equals()' #loc";
    }

    public BaseInspectionVisitor createVisitor(InspectionManager inspectionManager, boolean onTheFly) {
        return new ObjectEqualityVisitor(this, inspectionManager, onTheFly);
    }

    public InspectionGadgetsFix buildFix(PsiElement location) {
        return fix;
    }

    private class EqualityToEqualsFix extends InspectionGadgetsFix {
        public String getName() {
            return "Replace with .equals()";
        }

        public void applyFix(Project project, ProblemDescriptor descriptor) {
            final PsiElement comparisonToken = descriptor.getPsiElement();
            final PsiBinaryExpression
                    expression = (PsiBinaryExpression) comparisonToken.getParent();
            boolean negated = false;
            final PsiJavaToken sign = expression.getOperationSign();
            if (sign == null) {
                return;
            }
            if (sign.getTokenType() == JavaTokenType.NE) {
                negated = true;
            }
            final PsiExpression lhs = expression.getLOperand();
            if (lhs == null) {
                return;
            }
            final PsiExpression strippedLhs = ParenthesesUtils.stripParentheses(lhs);
            final PsiExpression rhs = expression.getROperand();
            if (rhs == null) {
                return;
            }
            final PsiExpression strippedRhs = ParenthesesUtils.stripParentheses(rhs);

            final String expString;
            if (ParenthesesUtils.getPrecendence(strippedLhs) > ParenthesesUtils.METHOD_CALL_PRECEDENCE) {
                expString = '(' + strippedLhs.getText() + ").equals(" + strippedRhs.getText() + ')';
            } else {
                expString = strippedLhs.getText() + ".equals(" + strippedRhs.getText() + ')';
            }
            final String newExpression;
            if (negated) {
                newExpression = '!' + expString;
            } else {
                newExpression = expString;
            }
            replaceExpression(project, expression, newExpression);
        }
    }

    private class ObjectEqualityVisitor extends BaseInspectionVisitor {
        private ObjectEqualityVisitor(BaseInspection inspection, InspectionManager inspectionManager, boolean isOnTheFly) {
            super(inspection, inspectionManager, isOnTheFly);
        }

        public void visitBinaryExpression(PsiBinaryExpression expression) {
            super.visitBinaryExpression(expression);
            if (!ComparisonUtils.isEqualityComparison(expression)) {
                return;
            }
            final PsiExpression rhs = expression.getROperand();
            if (!isObjectType(rhs)) {
                return;
            }

            final PsiExpression lhs = expression.getLOperand();
            if (!isObjectType(lhs)) {
                return;
            }
            if (m_ignoreEnums && isEnumType(rhs) && isEnumType(lhs)) {
                return;
            }
            final PsiMethod method = (PsiMethod) PsiTreeUtil.getParentOfType(expression, PsiMethod.class);
            final String methodName = method.getName();
            if ("equals".equals(methodName)) {
                return;
            }
            final PsiJavaToken sign = expression.getOperationSign();
            if (sign == null) {
                return;
            }
            registerError(sign);
        }

        private boolean isEnumType(PsiExpression exp) {
            if (exp == null) {
                return false;
            }

            final PsiType type = exp.getType();
            if (type == null) {
                return false;
            }
            if(!(type instanceof PsiClassType))
            {
                return false;
            }
            final PsiClass aClass = ((PsiClassType)type).resolve();
            if(aClass == null)
            {
                return false;
            }
            return aClass.isEnum();
        }

        private  boolean isObjectType(PsiExpression exp) {
            if (exp == null) {
                return false;
            }

            final PsiType type = exp.getType();
            if (type == null) {
                return false;
            }
            return !ClassUtils.isPrimitive(type)
                    && !type.equals(PsiType.NULL)
                    && !TypeUtils.isJavaLangString(type);
        }

    }

}

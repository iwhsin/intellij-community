package com.siyeh.ig.performance;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.siyeh.ig.*;

import java.util.HashSet;
import java.util.Set;

public class UnnecessaryTemporaryOnConversionToStringInspection extends ExpressionInspection {
    private static final Set s_basicTypes = new HashSet(6);

    static {
        s_basicTypes.add("java.lang.Short");
        s_basicTypes.add("java.lang.Integer");
        s_basicTypes.add("java.lang.Long");
        s_basicTypes.add("java.lang.Float");
        s_basicTypes.add("java.lang.Double");
        s_basicTypes.add("java.lang.Boolean");
    }

    public String getDisplayName() {
        return "Unnecessary temporary object in conversion to String";
    }

    public String getGroupDisplayName() {
        return GroupNames.PERFORMANCE_GROUP_NAME;
    }

    public String buildErrorString(PsiElement location) {
        final String replacementString = calculateReplacementExpression(location);
        return "#ref can be simplified to " + replacementString + " #loc";
    }

    private static String calculateReplacementExpression(PsiElement location) {
        final PsiMethodCallExpression expression = (PsiMethodCallExpression) location;
        final PsiReferenceExpression methodExpression = expression.getMethodExpression();
        final PsiNewExpression qualifier = (PsiNewExpression) methodExpression.getQualifierExpression();
        final PsiExpressionList argumentList = qualifier.getArgumentList();
        final PsiExpression arg = argumentList.getExpressions()[0];
        final PsiType type = qualifier.getType();
        final String qualifierType = type.getPresentableText();
        return qualifierType + ".toString(" + arg.getText() + ')';
    }

    public InspectionGadgetsFix buildFix(PsiElement location) {
        return new UnnecessaryTemporaryObjectFix((PsiMethodCallExpression) location);
    }

    private static class UnnecessaryTemporaryObjectFix extends InspectionGadgetsFix {
        private final String m_name;

        private UnnecessaryTemporaryObjectFix(PsiMethodCallExpression expression) {
            super();
            m_name = "Replace  with " + calculateReplacementExpression(expression);
        }

        public String getName() {
            return m_name;
        }

        public void applyFix(Project project, ProblemDescriptor descriptor) {
            final PsiMethodCallExpression expression = (PsiMethodCallExpression) descriptor.getPsiElement();
            final String newExpression = calculateReplacementExpression(expression);
            replaceExpression(project, expression, newExpression);
        }

    }

    public BaseInspectionVisitor createVisitor(InspectionManager inspectionManager, boolean onTheFly) {
        return new UnnecessaryTemporaryObjectVisitor(this, inspectionManager, onTheFly);
    }

    private static class UnnecessaryTemporaryObjectVisitor extends BaseInspectionVisitor {
        private UnnecessaryTemporaryObjectVisitor(BaseInspection inspection, InspectionManager inspectionManager, boolean isOnTheFly) {
            super(inspection, inspectionManager, isOnTheFly);
        }

        public void visitMethodCallExpression(PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);
            final PsiReferenceExpression methodExpression = expression.getMethodExpression();
            if (methodExpression == null) {
                return;
            }
            final String methodName = methodExpression.getReferenceName();
            if (!"toString".equals(methodName)) {
                return;
            }
            final PsiExpression qualifier = methodExpression.getQualifierExpression();
            if (qualifier == null) {
                return;
            }
            if (!(qualifier instanceof PsiNewExpression)) {
                return;
            }
            final PsiType type = qualifier.getType();
            if (type == null) {
                return;
            }
            final String typeName = type.getCanonicalText();
            if (!s_basicTypes.contains(typeName)) {
                return;
            }
            registerError(expression);
        }
    }

}

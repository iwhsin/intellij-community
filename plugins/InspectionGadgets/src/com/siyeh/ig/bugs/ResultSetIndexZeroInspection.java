package com.siyeh.ig.bugs;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.psi.*;
import com.intellij.psi.util.IsConstantExpressionVisitor;
import com.intellij.psi.util.ConstantExpressionUtil;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.ExpressionInspection;
import com.siyeh.ig.GroupNames;
import com.siyeh.ig.psiutils.TypeUtils;

public class ResultSetIndexZeroInspection extends ExpressionInspection {

    public String getDisplayName() {
        return "Use on index 0 with JDBC ResultSet";
    }

    public String getGroupDisplayName() {
        return GroupNames.BUGS_GROUP_NAME;
    }

    public String buildErrorString(PsiElement location) {
        return "Use on index 0 with JDBC ResultSet #loc";
    }

    public BaseInspectionVisitor createVisitor(InspectionManager inspectionManager, boolean onTheFly) {
        return new ResultSetIndexZeroVisitor(this, inspectionManager, onTheFly);
    }

    private static class ResultSetIndexZeroVisitor extends BaseInspectionVisitor {
        private ResultSetIndexZeroVisitor(BaseInspection inspection, InspectionManager inspectionManager, boolean isOnTheFly) {
            super(inspection, inspectionManager, isOnTheFly);
        }

        public void visitMethodCallExpression(PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);
            final PsiReferenceExpression methodExpression = expression.getMethodExpression();
            if (methodExpression == null) {
                return;
            }
            final String methodName = methodExpression.getReferenceName();
            if (!methodName.startsWith("get") && !methodName.startsWith("update") ) {
                return;
            }
            final PsiExpressionList argumentList = expression.getArgumentList();
            if (argumentList == null) {
                return;
            }
            final PsiExpression[] args = argumentList.getExpressions();
            if (args == null) {
                return;
            }
            if(args.length== 0)
            {
                return;
            }
            final PsiExpression arg = args[0];
            if (!TypeUtils.expressionHasType("int", arg)) {
                return;
            }
            if(!isConstant(arg))
            {
                return;
            }
            final Integer val = (Integer) ConstantExpressionUtil.computeCastTo(arg, PsiType.INT);
            if(val == null || val.intValue()!=0)
            {
                return;
            }
            final PsiExpression qualifier = methodExpression.getQualifierExpression();
            if (!TypeUtils.expressionHasTypeOrSubtype("java.sql.ResultSet", qualifier)) {
                return;
            }
            registerError(arg);
        }

        private static boolean isConstant(PsiExpression expression) {
            if (expression == null) {
                return false;
            }
            final IsConstantExpressionVisitor visitor =
                    new IsConstantExpressionVisitor();
            expression.accept(visitor);
            return visitor.isConstant();
        }

    }

}

package com.siyeh.ig.psiutils;

import com.intellij.psi.*;

public class VariableReturnedVisitor extends PsiRecursiveElementVisitor {
    private boolean returned = false;
    private final PsiVariable variable;

    public VariableReturnedVisitor(PsiVariable variable) {
        super();
        this.variable = variable;
    }

    public void visitReferenceExpression(PsiReferenceExpression ref) {
        final PsiExpression qualifier = ref.getQualifierExpression();
        if (qualifier != null) {
            qualifier.accept(this);
        }
        final PsiReferenceParameterList typeParameters = ref.getParameterList();
        if (typeParameters != null) {
            typeParameters.accept(this);
        }
    }

    public void visitReturnStatement(PsiReturnStatement returnStatement) {
        super.visitReturnStatement(returnStatement);
        final PsiExpression returnValue = returnStatement.getReturnValue();
        if (returnValue == null) {
            return;
        }
        if (!(returnValue instanceof PsiReferenceExpression)) {
            return;
        }
        final PsiElement referent = ((PsiReference) returnValue).resolve();
        if (referent == null) {
            return;
        }
        if (referent.equals(variable)) {
            returned = true;
        }
    }

    public boolean isReturned() {
        return returned;
    }
}

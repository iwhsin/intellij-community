package com.siyeh.ig.naming;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.siyeh.ig.*;
import com.siyeh.ig.fixes.RenameFix;

public class MethodNameSameAsClassNameInspection extends MethodInspection {

    public String getDisplayName() {
        return "Method name same as class name";
    }

    public String getGroupDisplayName() {
        return GroupNames.NAMING_CONVENTIONS_GROUP_NAME;
    }

    protected InspectionGadgetsFix buildFix(PsiElement location) {
        final RenameFix fix = new RenameFix();
        return fix;
    }

    protected boolean buildQuickFixesOnlyForOnTheFlyErrors() {
        return true;
    }

    public String buildErrorString(PsiElement location) {
        return "Method name '#ref' is the same as it's class name #loc";
    }

    public BaseInspectionVisitor createVisitor(InspectionManager inspectionManager, boolean onTheFly) {
        return new MethodNameSameAsClassNameVisitor(this, inspectionManager, onTheFly);
    }

    private static class MethodNameSameAsClassNameVisitor extends BaseInspectionVisitor {
        private MethodNameSameAsClassNameVisitor(BaseInspection inspection, InspectionManager inspectionManager, boolean isOnTheFly) {
            super(inspection, inspectionManager, isOnTheFly);
        }

        public void visitMethod(PsiMethod method) {
            // no call to super, so it doesn't drill down into inner classes
            if (method.isConstructor()) {
                return;
            }
            final String methodName = method.getName();
            if (methodName == null) {
                return;
            }
            final PsiClass containingClass = method.getContainingClass();
            if (containingClass == null) {
                return;
            }
            final String className = containingClass.getName();
            if (className == null) {
                return;
            }
            if (!methodName.equals(className)) {
                return;
            }
            registerMethodError(method);
        }

    }

}

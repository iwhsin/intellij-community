package com.siyeh.ig.classlayout;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifier;
import com.siyeh.ig.*;
import com.siyeh.ig.fixes.RemoveModifierFix;

public class FinalClassInspection extends MethodInspection {
    private static final Logger s_logger =
            Logger.getInstance("FinalClassInspection");

    public String getDisplayName() {
        return "'final' class";
    }

    public String getGroupDisplayName() {
        return GroupNames.CLASSLAYOUT_GROUP_NAME;
    }

    public String buildErrorString(PsiElement location) {
        return "Class declared '#ref' #loc";
    }

    public BaseInspectionVisitor createVisitor(InspectionManager inspectionManager, boolean onTheFly) {
        return new FinalStaticMethodVisitor(this, inspectionManager, onTheFly);
    }

    public InspectionGadgetsFix buildFix(PsiElement location) {
        return new RemoveModifierFix(location);
    }

    private static class FinalStaticMethodVisitor extends BaseInspectionVisitor {
        private FinalStaticMethodVisitor(BaseInspection inspection, InspectionManager inspectionManager, boolean isOnTheFly) {
            super(inspection, inspectionManager, isOnTheFly);
        }

        public void visitClass(PsiClass aClass) {
            //no call to super, so we don't drill into inner classes
            if (!aClass.hasModifierProperty(PsiModifier.FINAL)) {
                return;
            }
            registerModifierError(PsiModifier.FINAL, aClass);
        }

    }

}

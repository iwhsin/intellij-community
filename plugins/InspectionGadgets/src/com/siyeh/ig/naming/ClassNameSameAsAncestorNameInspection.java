package com.siyeh.ig.naming;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.siyeh.ig.*;
import com.siyeh.ig.fixes.RenameFix;

import java.util.HashSet;
import java.util.Set;

public class ClassNameSameAsAncestorNameInspection extends ClassInspection {
    private final RenameFix fix = new RenameFix();

    public String getDisplayName() {
        return "Class name same as ancestor name";
    }

    public String getGroupDisplayName() {
        return GroupNames.NAMING_CONVENTIONS_GROUP_NAME;
    }

    protected InspectionGadgetsFix buildFix(PsiElement location) {
        return fix;
    }

    protected boolean buildQuickFixesOnlyForOnTheFlyErrors() {
        return true;
    }

    public String buildErrorString(PsiElement location) {
        return "Class name '#ref' is the same as one of it's superclass' names #loc";
    }

    public BaseInspectionVisitor createVisitor(InspectionManager inspectionManager, boolean onTheFly) {
        return new ClassNameSameAsAncestorNameVisitor(this, inspectionManager, onTheFly);
    }

    private static class ClassNameSameAsAncestorNameVisitor extends BaseInspectionVisitor {
        private ClassNameSameAsAncestorNameVisitor(BaseInspection inspection, InspectionManager inspectionManager, boolean isOnTheFly) {
            super(inspection, inspectionManager, isOnTheFly);
        }

        public void visitClass(PsiClass aClass) {
            // no call to super, so it doesn't drill down into inner classes

            final String className = aClass.getName();

            final Set alreadyVisited = new HashSet(8);
            final PsiClass[] supers = aClass.getSupers();
            for (int i = 0; i < supers.length; i++) {
                final PsiClass aSuper = supers[i];
                if (hasMatchingName(aSuper, className, alreadyVisited)) {
                    registerClassError(aClass);
                }
            }
        }


        private static boolean hasMatchingName(PsiClass aSuper, String className,
                                               Set alreadyVisited) {
            if (alreadyVisited.contains(aSuper)) {
                return false;
            }
            alreadyVisited.add(aSuper);
            final String superName = aSuper.getName();
            if (superName.equals(className)) {
                return true;
            }
            final PsiClass[] supers = aSuper.getSupers();
            for (int i = 0; i < supers.length; i++) {
                if (hasMatchingName(supers[i], className, alreadyVisited)) {
                    return true;
                }
            }
            return false;
        }

    }

}

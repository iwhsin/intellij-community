package com.siyeh.ig.serialization;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.ClassInspection;
import com.siyeh.ig.GroupNames;
import com.siyeh.ig.psiutils.SerializationUtils;

public class ExternalizableWithSerializationMethodsInspection extends ClassInspection {

    public String getDisplayName() {
        return "Externalizable class with 'readObject()' or 'writeObject()'";
    }

    public String getGroupDisplayName() {
        return GroupNames.SERIALIZATION_GROUP_NAME;
    }

    public String buildErrorString(PsiElement location) {
        final PsiClass aClass = (PsiClass) location.getParent();
        final PsiMethod[] methods = aClass.getMethods();
        boolean hasReadObject = false;
        boolean hasWriteObject = false;
        for (int i = 0; i < methods.length; i++) {
            final PsiMethod method = methods[i];
            if (SerializationUtils.isReadObject(method)) {
                hasReadObject = true;
            } else if (SerializationUtils.isWriteObject(method)) {
                hasWriteObject = true;
            }
        }
        if (hasReadObject && hasWriteObject) {
            return "Externalizable class #ref defines readObject() and writeObject() #loc";
        } else if (hasWriteObject) {
            return "Externalizable class #ref defines writeObject() #loc";
        } else {
            return "Externalizable class #ref defines readObject() #loc";
        }
    }

    public BaseInspectionVisitor createVisitor(InspectionManager inspectionManager, boolean onTheFly) {
        return new ExternalizableDefinesSerializationMethodsVisitor(this, inspectionManager, onTheFly);
    }

    private static class ExternalizableDefinesSerializationMethodsVisitor extends BaseInspectionVisitor {
        private ExternalizableDefinesSerializationMethodsVisitor(BaseInspection inspection, InspectionManager inspectionManager, boolean isOnTheFly) {
            super(inspection, inspectionManager, isOnTheFly);
        }

        public void visitClass(PsiClass aClass) {
            // no call to super, so it doesn't drill down
            if (aClass.isInterface() || aClass.isAnnotationType()) {
                return;
            }
            if (!SerializationUtils.isExternalizable(aClass)) {
                return;
            }
            final PsiMethod[] methods = aClass.getMethods();
            boolean hasReadObject = false;
            boolean hasWriteObject = false;
            for (int i = 0; i < methods.length; i++) {
                final PsiMethod method = methods[i];
                if (SerializationUtils.isReadObject(method)) {
                    hasReadObject = true;
                } else if (SerializationUtils.isWriteObject(method)) {
                    hasWriteObject = true;
                }
            }
            if (!hasWriteObject && !hasReadObject) {
                return;
            }
            registerClassError(aClass);
        }

    }

}

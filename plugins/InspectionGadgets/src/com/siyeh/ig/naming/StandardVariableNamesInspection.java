package com.siyeh.ig.naming;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.siyeh.ig.*;
import com.siyeh.ig.fixes.RenameFix;

import java.util.HashMap;
import java.util.Map;

public class StandardVariableNamesInspection extends VariableInspection {

    private static final Map s_expectedTypes = new HashMap(10);
    private final RenameFix fix = new RenameFix();

    static {
        s_expectedTypes.put("b", "byte");
        s_expectedTypes.put("c", "char");
        s_expectedTypes.put("ch", "char");
        s_expectedTypes.put("d", "double");
        s_expectedTypes.put("f", "float");
        s_expectedTypes.put("i", "int");
        s_expectedTypes.put("j", "int");
        s_expectedTypes.put("k", "int");
        s_expectedTypes.put("m", "int");
        s_expectedTypes.put("n", "int");
        s_expectedTypes.put("l", "long");
        s_expectedTypes.put("s", "java.lang.String");
        s_expectedTypes.put("str", "java.lang.String");
    }

    public String getDisplayName() {
        return "Standard variable names";
    }

    protected InspectionGadgetsFix buildFix(PsiElement location) {
        return fix;
    }

    protected boolean buildQuickFixesOnlyForOnTheFlyErrors() {
        return true;
    }

    public String getGroupDisplayName() {
        return GroupNames.NAMING_CONVENTIONS_GROUP_NAME;
    }

    public String buildErrorString(PsiElement location) {
        final String variableName = location.getText();
        final String expectedType = (String) s_expectedTypes.get(variableName);
        return "Variable name '#ref' doesn't have type " + expectedType + " #loc";
    }

    public BaseInspectionVisitor createVisitor(InspectionManager inspectionManager, boolean onTheFly) {
        return new ExceptionNameDoesntEndWithExceptionVisitor(this, inspectionManager, onTheFly);
    }

    private static class ExceptionNameDoesntEndWithExceptionVisitor extends BaseInspectionVisitor {
        private ExceptionNameDoesntEndWithExceptionVisitor(BaseInspection inspection,
                                                           InspectionManager inspectionManager, boolean isOnTheFly) {
            super(inspection, inspectionManager, isOnTheFly);
        }

        public void visitVariable(PsiVariable var) {
            super.visitVariable(var);
            final String variableName = var.getName();
            final String expectedType = (String) s_expectedTypes.get(variableName);
            if (expectedType == null) {
                return;
            }
            final PsiType type = var.getType();
            if (type == null) {
                return;
            }
            final String typeText = type.getCanonicalText();
            if (typeText.equals(expectedType)) {
                return;
            }
            registerVariableError(var);
        }


    }

}

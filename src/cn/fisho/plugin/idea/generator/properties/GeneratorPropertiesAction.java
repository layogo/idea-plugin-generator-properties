package cn.fisho.plugin.idea.generator.properties;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 根据属性生成静态字段并包含在Properties的内部类中。
 * @author fisho
 * @date 2018-06-01
 */
public class GeneratorPropertiesAction extends AnAction {

    public static final String INNER_PROPERTIES_CLASS_NAME = "Properties";
    public static final String FIELD_FORMAT = "public static final String %s = \"%s\";";
    public static final int ASCII_A = 97;
    public static final int ASCII_Z = 122;
    public static final int ASCII_LOWER_UPPER_DIFF = 32;

    @Override
    public void actionPerformed(AnActionEvent e) {
        this.generatorProperties(this.getPsiMethodFromContext(e));
    }

    private void generatorProperties(final PsiClass psiMethod){
        (new WriteCommandAction.Simple(psiMethod.getProject(), new PsiFile[]{psiMethod.getContainingFile()}) {
            @Override
            protected void run() throws Throwable {
                GeneratorPropertiesAction.this.createProperties(psiMethod);
            }
        }).execute();
    }

    private void createProperties(PsiClass psiClass){

        final PsiField[] fields = psiClass.getFields();

        if (fields.length == 0){
            return;
        }

        PsiClass[] innerClasses = psiClass.getAllInnerClasses();

        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());

        PsiClass propertiesClass = null;

        for (PsiClass clazz: innerClasses){

            if (clazz.getQualifiedName().contains(INNER_PROPERTIES_CLASS_NAME)){
                propertiesClass = clazz;
            }

        }

        boolean createNewProperties = false;
        if (propertiesClass == null){
            propertiesClass = elementFactory.createClass(INNER_PROPERTIES_CLASS_NAME);
            createNewProperties = true;
        }

        PsiElementFactory innerElementFactory = JavaPsiFacade.getElementFactory(propertiesClass.getProject());
        Set<String> innerFields = Arrays.stream(propertiesClass.getAllFields()).map(f -> f.getName()).collect(Collectors.toSet());

        for (PsiField f: fields){

            //skip upper
            if(f.getName().charAt(0) < ASCII_A){
                continue;
            }

            final String name = captureName(f.getName());
            if (innerFields.contains(name)){
                continue;
            }

            propertiesClass.add(innerElementFactory.createFieldFromText(String.format(FIELD_FORMAT, name, f.getName()), propertiesClass));
        }

        if (createNewProperties){
            psiClass.add(propertiesClass);
        }


    }

    private String captureName(String name) {
        char[] cs = name.toCharArray();
        if (cs[0] >= ASCII_A && cs[0]<= ASCII_Z){
            cs[0] -= ASCII_LOWER_UPPER_DIFF;
        }
        return String.valueOf(cs);
    }

    private PsiClass getPsiMethodFromContext(AnActionEvent e) {
        PsiElement elementAt = this.getPsiElement(e);
        return elementAt == null ? null : (PsiClass) PsiTreeUtil.getParentOfType(elementAt, PsiClass.class);
    }

    private PsiElement getPsiElement(AnActionEvent e) {
        PsiFile psiFile = (PsiFile) e.getData(LangDataKeys.PSI_FILE);
        Editor editor = (Editor) e.getData(PlatformDataKeys.EDITOR);
        if (psiFile != null && editor != null) {
            int offset = editor.getCaretModel().getOffset();
            return psiFile.findElementAt(offset);
        } else {
            e.getPresentation().setEnabled(false);
            return null;
        }
    }


}

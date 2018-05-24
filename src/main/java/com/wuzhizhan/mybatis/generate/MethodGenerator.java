package com.wuzhizhan.mybatis.generate;

import com.google.common.base.Optional;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.GenericDomValue;
import com.wuzhizhan.mybatis.dom.model.Mapper;
import com.wuzhizhan.mybatis.dom.model.ParameterMap;
import com.wuzhizhan.mybatis.dom.model.ResultMap;
import com.wuzhizhan.mybatis.service.EditorService;
import com.wuzhizhan.mybatis.util.JavaUtils;
import com.wuzhizhan.mybatis.util.MapperUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Objects;

/**
 * @author Sealin
 * Created by Sealin on 2018-05-21.
 */
public class MethodGenerator {
    public static void generateMethod(GenericDomValue domValue) {
        Project project = domValue.getManager().getProject();
        Mapper mapper = MapperUtils.getMapper(domValue);
        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
        PsiElementFactory psiElementFactory = psiFacade.getElementFactory();

        String parameterTypeName = null;
        XmlTag tmpTag;
        if ((tmpTag = domValue.getXmlTag()).getAttributeValue("parameterMap") == null) {
            parameterTypeName = tmpTag.getAttributeValue("parameterType");
        } else {
            String paraMapName = tmpTag.getAttributeValue("parameterMap");
            for (ParameterMap parameterMap : mapper.getParameterMaps()) {
                if (Objects.equals(parameterMap.getId().getRawText(), paraMapName)) {
                    parameterTypeName = parameterMap.getType().getRawText();
                }
            }
        }
        String resultTypeName = null;
        if (tmpTag.getAttributeValue("resultMap") == null) {
            resultTypeName = tmpTag.getAttributeValue("resultType");
        } else {
            String resultMapName = tmpTag.getAttributeValue("resultMap");
            for (ResultMap resultMap : mapper.getResultMaps()) {
                if (Objects.equals(resultMap.getId().getRawText(), resultMapName)) {
                    resultTypeName = resultMap.getType().getRawText();
                }
            }
        }
        Optional<PsiClass> parameterClazz = StringUtils.isEmpty(parameterTypeName)
                ? Optional.absent()
                : JavaUtils.findClazz(project, parameterTypeName);
        Optional<PsiClass> resultClazz = StringUtils.isEmpty(resultTypeName)
                ? Optional.absent()
                : JavaUtils.findClazz(project, resultTypeName);

        Optional<PsiClass> mapperClazz = JavaUtils.findClazz(project, Objects.requireNonNull(mapper.getNamespace().getRawText()));
        PsiJavaFile file = (PsiJavaFile) mapperClazz.get().getContainingFile();
        PsiMethod method = psiElementFactory.createMethodFromText(
                String.format("%s %s(%s %s);"
                        , resultClazz.isPresent() ? resultClazz.get().getName() : "void"
                        , domValue.toString()
                        , parameterClazz.isPresent() ? parameterClazz.get().getName() : ""
                        , parameterClazz.isPresent() ? Objects.requireNonNull(parameterClazz.get().getName()).toLowerCase() : ""
                )
                , file.getOriginalElement().getLastChild()
        );
        file.getOriginalElement().getLastChild().add(method);
        EditorService editorService = EditorService.getInstance(project);
        editorService.scrollTo(file, file.getTextRange().getEndOffset() - method.getText().length() - 1);
    }

    public static void generateParameter(GenericDomValue domValue) {
        Project project = domValue.getManager().getProject();
        Optional<PsiClass> clazz = JavaUtils.findClazz(project, Objects.requireNonNull(MapperUtils.getMapper(domValue).getNamespace().getRawText()));
        if (!clazz.isPresent()) {
            return;
        }
        for (PsiMethod psiMethod : clazz.get().getMethods()) {
            if (psiMethod.getName().equals(domValue.toString())) {
                psiMethod.delete();
            }
        }
        generateMethod(domValue);
    }
}

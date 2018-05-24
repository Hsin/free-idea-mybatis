package com.wuzhizhan.mybatis.inspection;

import com.google.common.base.Optional;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.highlighting.BasicDomElementsInspection;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;
import com.intellij.util.xml.highlighting.DomHighlightingHelper;
import com.wuzhizhan.mybatis.dom.model.Mapper;
import com.wuzhizhan.mybatis.dom.model.ResultMap;
import com.wuzhizhan.mybatis.util.JavaUtils;
import com.wuzhizhan.mybatis.util.MapperUtils;

import java.util.Objects;

/**
 * @author yanglin
 */
public class MapperXmlInspection extends BasicDomElementsInspection<DomElement> {
    private Mapper mapper;
    private Optional<PsiClass> optionalClazz;

    public MapperXmlInspection() {
        super(DomElement.class);
    }

    @Override
    protected void checkDomElement(DomElement element, DomElementAnnotationHolder holder, DomHighlightingHelper helper) {
        initMapper(element);
        if (element instanceof GenericDomValue) {
            final GenericDomValue genericDomValue = (GenericDomValue) element;
            if (shouldCheckResolveProblems(genericDomValue)) {
                if (genericDomValue.toString().equals(genericDomValue.getXmlTag().getAttributeValue("id"))) {
                    if (!helper.checkResolveProblems(genericDomValue, holder).isEmpty()) {
                        holder.createProblem(element, String.format("no method \"#ref\" find in class %s"
                                , optionalClazz.get().getName())
                                , new MapperMethodQuickFix.MethodFixer(genericDomValue));
                    } else if (!checkParameter(genericDomValue)) {
                        holder.createProblem(
                                element
                                , String.format("method \"#ref\" can't find a parameter of type %s"
                                        , genericDomValue.getXmlTag().getAttributeValue("parameterType"))
                                , new MapperMethodQuickFix.ParameterFixer(genericDomValue));
                    } else if (!checkRet(genericDomValue)) {
                        holder.createProblem(
                                element
                                , String.format("method \"#ref\" not return result of type %s"
                                        , genericDomValue.getXmlTag().getAttributeValue("resultType") == null
                                                ? genericDomValue.getXmlTag().getAttributeValue("resultMap")
                                                : genericDomValue.getXmlTag().getAttributeValue("resultType")
                                )
                                , new MapperMethodQuickFix.ParameterFixer(genericDomValue));
                    }
                } else {
                    helper.checkResolveProblems(genericDomValue, holder);
                }
            }
        }
    }

    private void initMapper(DomElement element) {
        if (mapper == null || optionalClazz == null) {
            this.mapper = MapperUtils.getMapper(element);
            this.optionalClazz = JavaUtils.findClazz(
                    element.getManager().getProject(),
                    Objects.requireNonNull(mapper.getNamespace().getRawText())
            );
        }
    }

    private boolean checkParameter(GenericDomValue genericDomValue) {
        String parameterType = genericDomValue.getXmlTag().getAttributeValue("parameterType");
        // if no parameterType attribute
        if (parameterType == null) {
            return true;
        }
        Optional<PsiMethod> method = JavaUtils.findMethod(
                genericDomValue.getManager().getProject()
                , optionalClazz.get().getQualifiedName(), genericDomValue.toString()
        );
        if (!method.isPresent()) {
            return true;
        }
        for (PsiParameter psiParameter : method.get().getParameterList().getParameters()) {
            if (psiParameter.getType().getPresentableText().contains(parameterType) || psiParameter.getType().getCanonicalText().contains(parameterType)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkRet(GenericDomValue genericDomValue) {
        Project project = genericDomValue.getManager().getProject();
        Optional<PsiClass> clazz = JavaUtils.findClazz(project, Objects.requireNonNull(mapper.getNamespace().getRawText()));
        String xmlRetTypeName = null;
        XmlTag tmpTag = genericDomValue.getXmlTag();
        if (tmpTag.getAttributeValue("resultMap") == null) {
            xmlRetTypeName = tmpTag.getAttributeValue("resultType");
        } else {
            for (ResultMap resultMap : mapper.getResultMaps()) {
                if (Objects.equals(resultMap.getId().getRawText(), tmpTag.getAttributeValue("resultMap"))) {
                    xmlRetTypeName = resultMap.getType().getRawText();
                }
            }
        }
        if (StringUtil.isEmpty(xmlRetTypeName)) {
            return true;
        }
        for (PsiMethod psiMethod : clazz.get().getMethods()) {
            if (psiMethod.getName().equals(genericDomValue.toString())) {
                return Objects.requireNonNull(psiMethod.getReturnType()).getCanonicalText().contains(xmlRetTypeName);
            }
        }
        return true;
    }
}

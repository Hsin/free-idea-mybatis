package com.wuzhizhan.mybatis.inspection;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.util.xml.GenericDomValue;
import com.wuzhizhan.mybatis.generate.MethodGenerator;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * @author Sealin
 * Created by Sealin on 2018-05-19.
 */
public class MapperMethodQuickFix {
    static class MethodFixer extends GenericQuickFix {
        private GenericDomValue domValue;
        private boolean fixMode;

        /**
         * @param domValue domNode
         */
        public MethodFixer(GenericDomValue domValue) {
            this.domValue = domValue;
        }

        @Nls
        @NotNull
        @Override
        public String getName() {
            return "Create method";
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            MethodGenerator.generateMethod(domValue);
        }
    }


    static class ParameterFixer extends GenericQuickFix {
        private GenericDomValue domValue;

        public ParameterFixer(GenericDomValue domValue) {
            this.domValue = domValue;
        }

        @Nls
        @NotNull
        @Override
        public String getName() {
            return "Fix error type";
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            MethodGenerator.generateParameter(domValue);
        }
    }
}

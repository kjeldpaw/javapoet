/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.javapoet;

import com.google.testing.compile.Compiler;
import com.squareup.javapoet.compile.EvaluatingProcessor;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.List;

import static com.squareup.javapoet.TestUtil.findFirst;
import static com.squareup.javapoet.compile.EvaluatingProcessor.DUMMY;
import static javax.lang.model.util.ElementFilter.fieldsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class ParameterSpecTest {
    @Test
    public void equalsAndHashCode() {
        ParameterSpec a = ParameterSpec.builder(int.class, "foo").build();
        ParameterSpec b = ParameterSpec.builder(int.class, "foo").build();
        assertThat(a.equals(b)).isTrue();
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.toString()).isEqualTo(b.toString());
        a = ParameterSpec.builder(int.class, "i").addModifiers(Modifier.STATIC).build();
        b = ParameterSpec.builder(int.class, "i").addModifiers(Modifier.STATIC).build();
        assertThat(a.equals(b)).isTrue();
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.toString()).isEqualTo(b.toString());
    }

    @Test
    public void receiverParameterInstanceMethod() {
        ParameterSpec.Builder builder = ParameterSpec.builder(int.class, "this");
        assertThat(builder.build().name).isEqualTo("this");
    }

    @Test
    public void receiverParameterNestedClass() {
        ParameterSpec.Builder builder = ParameterSpec.builder(int.class, "Foo.this");
        assertThat(builder.build().name).isEqualTo("Foo.this");
    }

    @Test
    public void keywordName() {
        try {
            ParameterSpec.builder(int.class, "super");
            fail();
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("not a valid name: super");
        }
    }

    @Test
    public void nullAnnotationsAddition() {
        try {
            ParameterSpec.builder(int.class, "foo").addAnnotations(null);
            fail();
        } catch (Exception e) {
            assertThat(e.getMessage())
                    .isEqualTo("annotationSpecs == null");
        }
    }

    final class VariableElementFieldClass {
        String name;
    }

    @Test
    public void fieldVariableElement() throws Throwable{
        final var evaluatingProcessor = new EvaluatingProcessor((elements, types) -> {
            TypeElement classElement = elements.getTypeElement(VariableElementFieldClass.class.getCanonicalName());
            List<VariableElement> methods = fieldsIn(elements.getAllMembers(classElement));
            VariableElement element = findFirst(methods, "name");

            try {
                ParameterSpec.get(element);
                fail();
            } catch (IllegalArgumentException exception) {
                assertThat(exception).hasMessage("element is not a parameter");
            }
        });
        Compiler.javac().withProcessors(evaluatingProcessor).compile(DUMMY);
        evaluatingProcessor.throwIfStatementThrew();
    }

    final class VariableElementParameterClass {
        public void foo(@Nullable final String bar) {
        }
    }

    @Test
    public void parameterVariableElement() throws Throwable {
        final var evaluatingProcessor = new EvaluatingProcessor((elements, types) -> {
            TypeElement classElement = elements.getTypeElement(VariableElementParameterClass.class.getCanonicalName());
            List<ExecutableElement> methods = methodsIn(elements.getAllMembers(classElement));
            ExecutableElement element = findFirst(methods, "foo");
            VariableElement parameterElement = element.getParameters().get(0);

            assertThat(ParameterSpec.get(parameterElement)).hasToString("java.lang.String bar");
        });
        Compiler.javac().withProcessors(evaluatingProcessor).compile(DUMMY);
        evaluatingProcessor.throwIfStatementThrew();
    }

    @Test
    public void addNonFinalModifier() {
        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(Modifier.FINAL);
        modifiers.add(Modifier.PUBLIC);

        try {
            ParameterSpec.builder(int.class, "foo")
                    .addModifiers(modifiers);
            fail();
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("unexpected parameter modifier: public");
        }
    }

    @Test
    public void modifyAnnotations() {
        ParameterSpec.Builder builder = ParameterSpec.builder(int.class, "foo")
                .addAnnotation(Override.class)
                .addAnnotation(SuppressWarnings.class);

        builder.annotations.remove(1);
        assertThat(builder.build().annotations).hasSize(1);
    }

    @Test
    public void modifyModifiers() {
        ParameterSpec.Builder builder = ParameterSpec.builder(int.class, "foo")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        builder.modifiers.remove(1);
        assertThat(builder.build().modifiers).containsExactly(Modifier.PUBLIC);
    }
}

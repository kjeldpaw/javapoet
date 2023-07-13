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

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnnotatedTypeNameTest {

    private final static String NN = NeverNull.class.getCanonicalName();
    private final AnnotationSpec NEVER_NULL = AnnotationSpec.builder(NeverNull.class).build();
    private final static String TUA = TypeUseAnnotation.class.getCanonicalName();
    private final AnnotationSpec TYPE_USE_ANNOTATION =
            AnnotationSpec.builder(TypeUseAnnotation.class).build();

    @Target(ElementType.TYPE_USE)
    public @interface NeverNull {
    }

    @Target(ElementType.TYPE_USE)
    public @interface TypeUseAnnotation {
    }


    @Test
    void nullAnnotationArray() {
        assertThrows(NullPointerException.class, () -> TypeName.BOOLEAN.annotated((AnnotationSpec[]) null));
    }

    @Test
    void nullAnnotationList() {
        assertThrows(NullPointerException.class, () -> TypeName.DOUBLE.annotated((List<AnnotationSpec>) null));
    }

    @Test
    void annotated() {
        TypeName simpleString = TypeName.get(String.class);
        assertThat(simpleString.isAnnotated()).isFalse();
        assertThat(TypeName.get(String.class)).isEqualTo(simpleString);

        TypeName annotated = simpleString.annotated(NEVER_NULL);
        assertThat(annotated.isAnnotated()).isTrue();
        assertThat(annotated).isEqualTo(annotated.annotated());
    }

    @Test
    void annotatedType() {
        TypeName type = TypeName.get(String.class);
        TypeName actual = type.annotated(TYPE_USE_ANNOTATION);
        assertThat(actual.toString()).hasToString("java.lang. @" + TUA + " String");
    }

    @Test
    void annotatedTwice() {
        TypeName type = TypeName.get(String.class);
        TypeName actual =
                type.annotated(NEVER_NULL)
                        .annotated(TYPE_USE_ANNOTATION);
        assertThat(actual)
                .hasToString("java.lang. @" + NN + " @" + TUA + " String");
    }

    @Test
    void annotatedParameterizedType() {
        TypeName type = ParameterizedTypeName.get(List.class, String.class);
        TypeName actual = type.annotated(TYPE_USE_ANNOTATION);
        assertThat(actual).hasToString("java.util. @" + TUA + " List<java.lang.String>");
    }

    @Test
    void annotatedArgumentOfParameterizedType() {
        TypeName type = TypeName.get(String.class).annotated(TYPE_USE_ANNOTATION);
        TypeName actual = ParameterizedTypeName.get(ClassName.get(List.class), type);
        assertThat(actual).hasToString("java.util.List<java.lang. @" + TUA + " String>");
    }

    @Test
    void annotatedWildcardTypeNameWithSuper() {
        TypeName type = TypeName.get(String.class).annotated(TYPE_USE_ANNOTATION);
        TypeName actual = WildcardTypeName.supertypeOf(type);
        assertThat(actual).hasToString("? super java.lang. @" + TUA + " String");
    }

    @Test
    void annotatedWildcardTypeNameWithExtends() {
        TypeName type = TypeName.get(String.class).annotated(TYPE_USE_ANNOTATION);
        TypeName actual = WildcardTypeName.subtypeOf(type);
        assertThat(actual).hasToString("? extends java.lang. @" + TUA + " String");
    }

    @Test
    void annotatedEquivalence() {
        annotatedEquivalence(TypeName.VOID);
        annotatedEquivalence(ArrayTypeName.get(Object[].class));
        annotatedEquivalence(ClassName.get(Object.class));
        annotatedEquivalence(ParameterizedTypeName.get(List.class, Object.class));
        annotatedEquivalence(TypeVariableName.get(Object.class));
        annotatedEquivalence(WildcardTypeName.get(Object.class));
    }

    private void annotatedEquivalence(TypeName type) {
        assertThat(type.isAnnotated()).isFalse();
        assertThat(type).isEqualTo(type);
        assertThat(type.annotated(TYPE_USE_ANNOTATION)).isEqualTo(type.annotated(TYPE_USE_ANNOTATION));
        assertThat(type).isNotEqualTo(type.annotated(TYPE_USE_ANNOTATION));
        assertThat(type).hasSameHashCodeAs(type);
        assertThat(type.annotated(TYPE_USE_ANNOTATION).hashCode()).isEqualTo(
                type.annotated(TYPE_USE_ANNOTATION).hashCode());
        assertThat(type.hashCode()).isNotEqualTo(type.annotated(TYPE_USE_ANNOTATION).hashCode());
    }

    // https://github.com/square/javapoet/issues/431
    @Test
    void annotatedNestedType() {
        TypeName type = TypeName.get(Map.Entry.class).annotated(TYPE_USE_ANNOTATION);
        assertThat(type).hasToString("java.util.Map. @" + TUA + " Entry");
    }

    @Test
    void annotatedEnclosingAndNestedType() {
        TypeName type = ((ClassName) TypeName.get(Map.class).annotated(TYPE_USE_ANNOTATION))
                .nestedClass("Entry").annotated(TYPE_USE_ANNOTATION);
        assertThat(type).hasToString("java.util. @" + TUA + " Map. @" + TUA + " Entry");
    }

    // https://github.com/square/javapoet/issues/431
    @Test
    void annotatedNestedParameterizedType() {
        TypeName type = ParameterizedTypeName.get(Map.Entry.class, Byte.class, Byte.class)
                .annotated(TYPE_USE_ANNOTATION);
        assertThat(type)
                .hasToString("java.util.Map. @" + TUA + " Entry<java.lang.Byte, java.lang.Byte>");
    }

    @Test
    void withoutAnnotationsOnAnnotatedEnclosingAndNestedType() {
        TypeName type = ((ClassName) TypeName.get(Map.class).annotated(TYPE_USE_ANNOTATION))
                .nestedClass("Entry").annotated(TYPE_USE_ANNOTATION);
        assertThat(type.isAnnotated()).isTrue();
        assertThat(type.withoutAnnotations()).isEqualTo(TypeName.get(Map.Entry.class));
    }

    @Test
    void withoutAnnotationsOnAnnotatedEnclosingType() {
        TypeName type = ((ClassName) TypeName.get(Map.class).annotated(TYPE_USE_ANNOTATION))
                .nestedClass("Entry");
        assertThat(type.isAnnotated()).isTrue();
        assertThat(type.withoutAnnotations()).isEqualTo(TypeName.get(Map.Entry.class));
    }

    @Test
    void withoutAnnotationsOnAnnotatedNestedType() {
        TypeName type = ((ClassName) TypeName.get(Map.class))
                .nestedClass("Entry").annotated(TYPE_USE_ANNOTATION);
        assertThat(type.isAnnotated()).isTrue();
        assertThat(type.withoutAnnotations()).isEqualTo(TypeName.get(Map.Entry.class));
    }

    // https://github.com/square/javapoet/issues/614
    @Test
    void annotatedArrayType() {
        TypeName type = ArrayTypeName.of(ClassName.get(Object.class)).annotated(TYPE_USE_ANNOTATION);
        assertThat(type).hasToString("java.lang.Object @" + TUA + " []");
    }

    @Test
    void annotatedArrayElementType() {
        TypeName type = ArrayTypeName.of(ClassName.get(Object.class).annotated(TYPE_USE_ANNOTATION));
        assertThat(type).hasToString("java.lang. @" + TUA + " Object[]");
    }

    // https://github.com/square/javapoet/issues/614
    @Test
    void annotatedOuterMultidimensionalArrayType() {
        TypeName type = ArrayTypeName.of(ArrayTypeName.of(ClassName.get(Object.class)))
                .annotated(TYPE_USE_ANNOTATION);
        assertThat(type).hasToString("java.lang.Object @" + TUA + " [][]");
    }

    // https://github.com/square/javapoet/issues/614
    @Test
    void annotatedInnerMultidimensionalArrayType() {
        TypeName type = ArrayTypeName.of(ArrayTypeName.of(ClassName.get(Object.class))
                .annotated(TYPE_USE_ANNOTATION));
        assertThat(type).hasToString("java.lang.Object[] @" + TUA + " []");
    }

    // https://github.com/square/javapoet/issues/614
    @Test
    void annotatedArrayTypeVarargsParameter() {
        TypeName type = ArrayTypeName.of(ArrayTypeName.of(ClassName.get(Object.class)))
                .annotated(TYPE_USE_ANNOTATION);
        MethodSpec varargsMethod = MethodSpec.methodBuilder("m")
                .addParameter(
                        ParameterSpec.builder(type, "p")
                                .build())
                .varargs()
                .build();
        assertThat(varargsMethod).hasToString("void m(java.lang.Object @" + TUA + " []... p) {\n"
                + "}\n");
    }

    // https://github.com/square/javapoet/issues/614
    @Test
    void annotatedArrayTypeInVarargsParameter() {
        TypeName type = ArrayTypeName.of(ArrayTypeName.of(ClassName.get(Object.class))
                .annotated(TYPE_USE_ANNOTATION));
        MethodSpec varargsMethod = MethodSpec.methodBuilder("m")
                .addParameter(
                        ParameterSpec.builder(type, "p")
                                .build())
                .varargs()
                .build();
        assertThat(varargsMethod).hasToString("void m(java.lang.Object[] @" + TUA + " ... p) {\n"
                + "}\n");
    }
}

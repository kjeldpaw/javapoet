/*
 * Copyright (C) 2014 Google, Inc.
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

import java.util.Map;
import javax.lang.model.element.TypeElement;

import com.google.testing.compile.Compiler;
import com.squareup.javapoet.compile.EvaluatingProcessor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static com.squareup.javapoet.compile.EvaluatingProcessor.DUMMY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

class ClassNameTest {

    @Test
    void bestGuessForString_simpleClass() {
        assertThat(ClassName.bestGuess(String.class.getName()))
                .isEqualTo(ClassName.get("java.lang", "String"));
    }

    @Test
    void bestGuessNonAscii() {
        ClassName className = ClassName.bestGuess(
                "com.\ud835\udc1andro\ud835\udc22d.\ud835\udc00ctiv\ud835\udc22ty");
        assertThat("com.\ud835\udc1andro\ud835\udc22d").isEqualTo(className.packageName());
        assertThat("\ud835\udc00ctiv\ud835\udc22ty").isEqualTo(className.simpleName());
    }

    static class OuterClass {
        static class InnerClass {
        }
    }

    @Test
    void bestGuessForString_nestedClass() {
        assertThat(ClassName.bestGuess(Map.Entry.class.getCanonicalName()))
                .isEqualTo(ClassName.get("java.util", "Map", "Entry"));
        assertThat(ClassName.bestGuess(OuterClass.InnerClass.class.getCanonicalName()))
                .isEqualTo(ClassName.get("com.squareup.javapoet",
                        "ClassNameTest", "OuterClass", "InnerClass"));
    }

    @Test
    void bestGuessForString_defaultPackage() {
        assertThat(ClassName.bestGuess("SomeClass"))
                .isEqualTo(ClassName.get("", "SomeClass"));
        assertThat(ClassName.bestGuess("SomeClass.Nested"))
                .isEqualTo(ClassName.get("", "SomeClass", "Nested"));
        assertThat(ClassName.bestGuess("SomeClass.Nested.EvenMore"))
                .isEqualTo(ClassName.get("", "SomeClass", "Nested", "EvenMore"));
    }

    @Test
    void bestGuessForString_confusingInput() {
        assertBestGuessThrows("");
        assertBestGuessThrows(".");
        assertBestGuessThrows(".Map");
        assertBestGuessThrows("java");
        assertBestGuessThrows("java.util");
        assertBestGuessThrows("java.util.");
        assertBestGuessThrows("java..util.Map.Entry");
        assertBestGuessThrows("java.util..Map.Entry");
        assertBestGuessThrows("java.util.Map..Entry");
        assertBestGuessThrows("com.test.$");
        assertBestGuessThrows("com.test.LooksLikeAClass.pkg");
        assertBestGuessThrows("!@#$gibberish%^&*");
    }

    private void assertBestGuessThrows(String s) {
        try {
            ClassName.bestGuess(s);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    void createNestedClass() {
        ClassName foo = ClassName.get("com.example", "Foo");
        ClassName bar = foo.nestedClass("Bar");
        assertThat(bar).isEqualTo(ClassName.get("com.example", "Foo", "Bar"));
        ClassName baz = bar.nestedClass("Baz");
        assertThat(baz).isEqualTo(ClassName.get("com.example", "Foo", "Bar", "Baz"));
    }

    static class $Outer {
        static class $Inner {
        }
    }

    @Test
    void classNameFromTypeElement() throws Throwable {
        final var evaluatingProcessor = new EvaluatingProcessor((elements, types) -> {
            TypeElement object = elements.getTypeElement(Object.class.getCanonicalName());
            assertThat(ClassName.get(object).toString()).isEqualTo("java.lang.Object");
            TypeElement outer = elements.getTypeElement($Outer.class.getCanonicalName());
            assertThat(ClassName.get(outer).toString()).isEqualTo("com.squareup.javapoet.ClassNameTest.$Outer");
            TypeElement inner = elements.getTypeElement($Outer.$Inner.class.getCanonicalName());
            assertThat(ClassName.get(inner).toString()).isEqualTo("com.squareup.javapoet.ClassNameTest.$Outer.$Inner");
        });
        Compiler.javac().withProcessors(evaluatingProcessor).compile(DUMMY);
        evaluatingProcessor.throwIfStatementThrew();
    }

    /**
     * Buck builds with "source-based ABI generation" and those builds don't support
     * {@link TypeElement#getKind()}. Test to confirm that we don't use that API.
     */
    @Test
    void classNameFromTypeElementDoesntUseGetKind() throws Throwable{
        final var evaluatingProcessor = new EvaluatingProcessor((elements, types) -> {
            TypeElement object = elements.getTypeElement(Object.class.getCanonicalName());
            assertThat(ClassName.get(preventGetKind(object)).toString())
                    .isEqualTo("java.lang.Object");
            TypeElement outer = elements.getTypeElement($Outer.class.getCanonicalName());
            assertThat(ClassName.get(preventGetKind(outer)).toString())
                    .isEqualTo("com.squareup.javapoet.ClassNameTest.$Outer");
            TypeElement inner = elements.getTypeElement($Outer.$Inner.class.getCanonicalName());
            assertThat(ClassName.get(preventGetKind(inner)).toString())
                    .isEqualTo("com.squareup.javapoet.ClassNameTest.$Outer.$Inner");
        });
        Compiler.javac().withProcessors(evaluatingProcessor).compile(DUMMY);
        evaluatingProcessor.throwIfStatementThrew();
    }

    /**
     * Returns a new instance like {@code object} that throws on {@code getKind()}.
     */
    private TypeElement preventGetKind(TypeElement object) {
        TypeElement spy = Mockito.spy(object);
        when(spy.getKind()).thenThrow(new AssertionError());
        when(spy.getEnclosingElement()).thenAnswer(invocation -> {
            Object enclosingElement = invocation.callRealMethod();
            return enclosingElement instanceof TypeElement
                    ? preventGetKind((TypeElement) enclosingElement)
                    : enclosingElement;
        });
        return spy;
    }

    @Test
    void classNameFromClass() {
        assertThat(ClassName.get(Object.class).toString())
                .isEqualTo("java.lang.Object");
        assertThat(ClassName.get(OuterClass.InnerClass.class).toString())
                .isEqualTo("com.squareup.javapoet.ClassNameTest.OuterClass.InnerClass");
        assertThat((ClassName.get(new Object() {
        }.getClass())).toString())
                .isEqualTo("com.squareup.javapoet.ClassNameTest$1");
        assertThat((ClassName.get(new Object() {
            Object inner = new Object() {
            };
        }.inner.getClass())).toString())
                .isEqualTo("com.squareup.javapoet.ClassNameTest$2$1");
        assertThat((ClassName.get($Outer.class)).toString())
                .isEqualTo("com.squareup.javapoet.ClassNameTest.$Outer");
        assertThat((ClassName.get($Outer.$Inner.class)).toString())
                .isEqualTo("com.squareup.javapoet.ClassNameTest.$Outer.$Inner");
    }

    @Test
    void peerClass() {
        assertThat(ClassName.get(Double.class).peerClass("Short"))
                .isEqualTo(ClassName.get(Short.class));
        assertThat(ClassName.get("", "Double").peerClass("Short"))
                .isEqualTo(ClassName.get("", "Short"));
        assertThat(ClassName.get("a.b", "Combo", "Taco").peerClass("Burrito"))
                .isEqualTo(ClassName.get("a.b", "Combo", "Burrito"));
    }

    @Test
    void fromClassRejectionTypes() {
        try {
            ClassName.get(int.class);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
        try {
            ClassName.get(void.class);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
        try {
            ClassName.get(Object[].class);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    void reflectionName() {
        assertThat("java.lang.Object").isEqualTo(TypeName.OBJECT.reflectionName());
        assertThat("java.lang.Thread$State").isEqualTo(ClassName.get(Thread.State.class).reflectionName());
        assertThat("java.util.Map$Entry").isEqualTo(ClassName.get(Map.Entry.class).reflectionName());
        assertThat("Foo").isEqualTo(ClassName.get("", "Foo").reflectionName());
        assertThat("Foo$Bar$Baz").isEqualTo(ClassName.get("", "Foo", "Bar", "Baz").reflectionName());
        assertThat("a.b.c.Foo$Bar$Baz").isEqualTo(ClassName.get("a.b.c", "Foo", "Bar", "Baz").reflectionName());
    }

    @Test
    void canonicalName() {
        assertThat("java.lang.Object").isEqualTo(TypeName.OBJECT.canonicalName());
        assertThat("java.lang.Thread.State").isEqualTo(ClassName.get(Thread.State.class).canonicalName());
        assertThat("java.util.Map.Entry").isEqualTo(ClassName.get(Map.Entry.class).canonicalName());
        assertThat("Foo").isEqualTo(ClassName.get("", "Foo").canonicalName());
        assertThat("Foo.Bar.Baz").isEqualTo(ClassName.get("", "Foo", "Bar", "Baz").canonicalName());
        assertThat("a.b.c.Foo.Bar.Baz").isEqualTo(ClassName.get("a.b.c", "Foo", "Bar", "Baz").canonicalName());
    }
}

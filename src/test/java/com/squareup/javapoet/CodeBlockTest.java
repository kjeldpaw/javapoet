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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

final class CodeBlockTest {
    @Test
    void equalsAndHashCode() {
        CodeBlock a = CodeBlock.builder().build();
        CodeBlock b = CodeBlock.builder().build();
        assertThat(a.equals(b)).isTrue();
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        a = CodeBlock.builder().add("$L", "taco").build();
        b = CodeBlock.builder().add("$L", "taco").build();
        assertThat(a.equals(b)).isTrue();
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void of() {
        CodeBlock a = CodeBlock.of("$L taco", "delicious");
        assertThat(a.toString()).isEqualTo("delicious taco");
    }

    @Test
    void isEmpty() {
        assertThat(CodeBlock.builder().isEmpty()).isTrue();
        assertThat(CodeBlock.builder().add("").isEmpty()).isTrue();
        assertThat(CodeBlock.builder().add(" ").isEmpty()).isFalse();
    }

    @Test
    void indentCannotBeIndexed() {
        try {
            CodeBlock.builder().add("$1>", "taco").build();
            fail();
        } catch (IllegalArgumentException exp) {
            assertThat(exp)
                    .hasMessage("$$, $>, $<, $[, $], $W, and $Z may not have an index");
        }
    }

    @Test
    void deindentCannotBeIndexed() {
        try {
            CodeBlock.builder().add("$1<", "taco").build();
            fail();
        } catch (IllegalArgumentException exp) {
            assertThat(exp)
                    .hasMessage("$$, $>, $<, $[, $], $W, and $Z may not have an index");
        }
    }

    @Test
    void dollarSignEscapeCannotBeIndexed() {
        try {
            CodeBlock.builder().add("$1$", "taco").build();
            fail();
        } catch (IllegalArgumentException exp) {
            assertThat(exp)
                    .hasMessage("$$, $>, $<, $[, $], $W, and $Z may not have an index");
        }
    }

    @Test
    void statementBeginningCannotBeIndexed() {
        try {
            CodeBlock.builder().add("$1[", "taco").build();
            fail();
        } catch (IllegalArgumentException exp) {
            assertThat(exp)
                    .hasMessage("$$, $>, $<, $[, $], $W, and $Z may not have an index");
        }
    }

    @Test
    void statementEndingCannotBeIndexed() {
        try {
            CodeBlock.builder().add("$1]", "taco").build();
            fail();
        } catch (IllegalArgumentException exp) {
            assertThat(exp)
                    .hasMessage("$$, $>, $<, $[, $], $W, and $Z may not have an index");
        }
    }

    @Test
    void nameFormatCanBeIndexed() {
        CodeBlock block = CodeBlock.builder().add("$1N", "taco").build();
        assertThat(block.toString()).isEqualTo("taco");
    }

    @Test
    void literalFormatCanBeIndexed() {
        CodeBlock block = CodeBlock.builder().add("$1L", "taco").build();
        assertThat(block.toString()).isEqualTo("taco");
    }

    @Test
    void stringFormatCanBeIndexed() {
        CodeBlock block = CodeBlock.builder().add("$1S", "taco").build();
        assertThat(block.toString()).isEqualTo("\"taco\"");
    }

    @Test
    void typeFormatCanBeIndexed() {
        CodeBlock block = CodeBlock.builder().add("$1T", String.class).build();
        assertThat(block.toString()).isEqualTo("java.lang.String");
    }

    @Test
    void simpleNamedArgument() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("text", "taco");
        CodeBlock block = CodeBlock.builder().addNamed("$text:S", map).build();
        assertThat(block.toString()).isEqualTo("\"taco\"");
    }

    @Test
    void repeatedNamedArgument() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("text", "tacos");
        CodeBlock block = CodeBlock.builder()
                .addNamed("\"I like \" + $text:S + \". Do you like \" + $text:S + \"?\"", map)
                .build();
        assertThat(block.toString()).isEqualTo(
                "\"I like \" + \"tacos\" + \". Do you like \" + \"tacos\" + \"?\"");
    }

    @Test
    void namedAndNoArgFormat() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("text", "tacos");
        CodeBlock block = CodeBlock.builder()
                .addNamed("$>\n$text:L for $$3.50", map).build();
        assertThat(block.toString()).isEqualTo("\n  tacos for $3.50");
    }

    @Test
    void missingNamedArgument() {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            CodeBlock.builder().addNamed("$text:S", map).build();
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected).hasMessage("Missing named argument for $text");
        }
    }

    @Test
    void lowerCaseNamed() {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Text", "tacos");
            CodeBlock block = CodeBlock.builder().addNamed("$Text:S", map).build();
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected).hasMessage("argument 'Text' must start with a lowercase character");
        }
    }

    @Test
    void multipleNamedArguments() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("pipe", System.class);
        map.put("text", "tacos");

        CodeBlock block = CodeBlock.builder()
                .addNamed("$pipe:T.out.println(\"Let's eat some $text:L\");", map)
                .build();

        assertThat(block.toString()).isEqualTo(
                "java.lang.System.out.println(\"Let's eat some tacos\");");
    }

    @Test
    void namedNewline() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("clazz", Integer.class);
        CodeBlock block = CodeBlock.builder().addNamed("$clazz:T\n", map).build();
        assertThat(block.toString()).isEqualTo("java.lang.Integer\n");
    }

    @Test
    void danglingNamed() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("clazz", Integer.class);
        try {
            CodeBlock.builder().addNamed("$clazz:T$", map).build();
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected).hasMessage("dangling $ at end");
        }
    }

    @Test
    void indexTooHigh() {
        try {
            CodeBlock.builder().add("$2T", String.class).build();
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected).hasMessage("index 2 for '$2T' not in range (received 1 arguments)");
        }
    }

    @Test
    void indexIsZero() {
        try {
            CodeBlock.builder().add("$0T", String.class).build();
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected).hasMessage("index 0 for '$0T' not in range (received 1 arguments)");
        }
    }

    @Test
    void indexIsNegative() {
        try {
            CodeBlock.builder().add("$-1T", String.class).build();
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected).hasMessage("invalid format string: '$-1T'");
        }
    }

    @Test
    void indexWithoutFormatType() {
        try {
            CodeBlock.builder().add("$1", String.class).build();
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected).hasMessage("dangling format characters in '$1'");
        }
    }

    @Test
    void indexWithoutFormatTypeNotAtStringEnd() {
        try {
            CodeBlock.builder().add("$1 taco", String.class).build();
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected).hasMessage("invalid format string: '$1 taco'");
        }
    }

    @Test
    void indexButNoArguments() {
        try {
            CodeBlock.builder().add("$1T").build();
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected).hasMessage("index 1 for '$1T' not in range (received 0 arguments)");
        }
    }

    @Test
    void formatIndicatorAlone() {
        try {
            CodeBlock.builder().add("$", String.class).build();
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected).hasMessage("dangling format characters in '$'");
        }
    }

    @Test
    void formatIndicatorWithoutIndexOrFormatType() {
        try {
            CodeBlock.builder().add("$ tacoString", String.class).build();
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected).hasMessage("invalid format string: '$ tacoString'");
        }
    }

    @Test
    void sameIndexCanBeUsedWithDifferentFormats() {
        CodeBlock block = CodeBlock.builder()
                .add("$1T.out.println($1S)", ClassName.get(System.class))
                .build();
        assertThat(block).hasToString("java.lang.System.out.println(\"java.lang.System\")");
    }

    @Test
    void tooManyStatementEnters() {
        CodeBlock codeBlock = CodeBlock.builder().add("$[$[").build();
        try {
            // We can't report this error until rendering type because code blocks might be composed.
            codeBlock.toString();
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected).hasMessage("statement enter $[ followed by statement enter $[");
        }
    }

    @Test
    void statementExitWithoutStatementEnter() {
        CodeBlock codeBlock = CodeBlock.builder().add("$]").build();
        try {
            // We can't report this error until rendering type because code blocks might be composed.
            codeBlock.toString();
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected).hasMessage("statement exit $] has no matching statement enter $[");
        }
    }

    @Test
    void join() {
        List<CodeBlock> codeBlocks = new ArrayList<>();
        codeBlocks.add(CodeBlock.of("$S", "hello"));
        codeBlocks.add(CodeBlock.of("$T", ClassName.get("world", "World")));
        codeBlocks.add(CodeBlock.of("need tacos"));

        CodeBlock joined = CodeBlock.join(codeBlocks, " || ");
        assertThat(joined).hasToString("\"hello\" || world.World || need tacos");
    }

    @Test
    void joining() {
        List<CodeBlock> codeBlocks = new ArrayList<>();
        codeBlocks.add(CodeBlock.of("$S", "hello"));
        codeBlocks.add(CodeBlock.of("$T", ClassName.get("world", "World")));
        codeBlocks.add(CodeBlock.of("need tacos"));

        CodeBlock joined = codeBlocks.stream().collect(CodeBlock.joining(" || "));
        assertThat(joined.toString()).isEqualTo("\"hello\" || world.World || need tacos");
    }

    @Test
    void joiningSingle() {
        List<CodeBlock> codeBlocks = new ArrayList<>();
        codeBlocks.add(CodeBlock.of("$S", "hello"));

        CodeBlock joined = codeBlocks.stream().collect(CodeBlock.joining(" || "));
        assertThat(joined.toString()).isEqualTo("\"hello\"");
    }

    @Test
    void joiningWithPrefixAndSuffix() {
        List<CodeBlock> codeBlocks = new ArrayList<>();
        codeBlocks.add(CodeBlock.of("$S", "hello"));
        codeBlocks.add(CodeBlock.of("$T", ClassName.get("world", "World")));
        codeBlocks.add(CodeBlock.of("need tacos"));

        CodeBlock joined = codeBlocks.stream().collect(CodeBlock.joining(" || ", "start {", "} end"));
        assertThat(joined.toString()).isEqualTo("start {\"hello\" || world.World || need tacos} end");
    }

    @Test
    void clear() {
        CodeBlock block = CodeBlock.builder()
                .addStatement("$S", "Test string")
                .clear()
                .build();

        assertThat(block.toString()).isEmpty();
    }
}

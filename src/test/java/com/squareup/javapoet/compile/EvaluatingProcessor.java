package com.squareup.javapoet.compile;

import com.google.common.collect.ImmutableSet;
import com.google.testing.compile.JavaFileObjects;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

public class EvaluatingProcessor extends AbstractProcessor {
    public static final JavaFileObject DUMMY = JavaFileObjects.forSourceLines("Dummy", new String[]{"final class Dummy {}"});

    private final BiConsumer<Elements, Types> statement;
    private Throwable thrown;
    private Elements elements;
    private Types types;

    public EvaluatingProcessor(BiConsumer<Elements, Types> statement) {
        this.statement = statement;
    }

    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    public Set<String> getSupportedAnnotationTypes() {
        return ImmutableSet.of("*");
    }

    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elements = processingEnv.getElementUtils();
        this.types = processingEnv.getTypeUtils();
    }

    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            try {
                this.statement.accept(elements, types);
            } catch (Throwable throwable) {
                this.thrown = throwable;
            }
        }

        return false;
    }

    public void throwIfStatementThrew() throws Throwable {
        if (this.thrown != null) {
            throw this.thrown;
        }
    }

    public Optional<Throwable> getThrown() {
        return Optional.ofNullable(thrown);
    }
}

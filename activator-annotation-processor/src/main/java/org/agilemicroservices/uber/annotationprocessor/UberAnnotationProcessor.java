package org.agilemicroservices.uber.annotationprocessor;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;


@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("org.agilemicroservices.uber.util.annotation.UberActivator")
public class UberAnnotationProcessor extends AbstractProcessor {
    private ProcessingEnvironment processingEnv;
    private FileObject fileObject;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        try {
            fileObject = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "",
                    "META-INF/services/org.agilemicroservices.uber.Activator");
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create Uber service file.", e);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }

        String typeName = ((TypeElement)roundEnv.getRootElements().iterator().next()).getQualifiedName().toString();

        try (Writer writer = fileObject.openWriter()) {
            writer.write(typeName);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write to Uber service file.", e);
        }
        return false;
    }
}

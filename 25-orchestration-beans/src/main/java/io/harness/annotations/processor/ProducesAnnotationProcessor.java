package io.harness.annotations.processor;

import com.google.common.collect.ImmutableMap;

import io.harness.adviser.Adviser;
import io.harness.ambiance.Level;
import io.harness.annotations.Produces;
import io.harness.facilitator.Facilitator;
import io.harness.resolvers.Resolver;
import io.harness.state.State;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

public class ProducesAnnotationProcessor extends AbstractProcessor {
  private static final Map<String, String> producerClassMap =
      ImmutableMap.<String, String>builder()
          .put(Adviser.class.getCanonicalName(), Adviser.class.getCanonicalName())
          .put(Facilitator.class.getCanonicalName(), Facilitator.class.getCanonicalName())
          .put(Resolver.class.getCanonicalName(), Resolver.class.getCanonicalName())
          .put(State.class.getCanonicalName(), State.class.getCanonicalName())
          .put(Level.class.getCanonicalName(), Level.class.getCanonicalName())
          .build();

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(Produces.class)) {
      // Check if a class has been annotated with @Produces
      if (annotatedElement.getKind() != ElementKind.CLASS) {
        error(annotatedElement, "Only classes can be annotated with @%s", Produces.class.getSimpleName());
        return true;
      }

      if (!validateClass((TypeElement) annotatedElement)) {
        return true;
      }
    }

    return true;
  }

  private boolean validateClass(TypeElement classElement) {
    // Don't allow non-public classes with @Produces annotation.
    if (!classElement.getModifiers().contains(Modifier.PUBLIC)) {
      error(classElement, "The class %s is not public", classElement.getSimpleName().toString());
      return false;
    }

    // Don't allow abstract classes with @Produces annotation.
    if (classElement.getModifiers().contains(Modifier.ABSTRACT)) {
      error(classElement, "The class %s is abstract. You can't annotate abstract classes with @%",
          classElement.getSimpleName().toString(), Produces.class.getSimpleName());
      return false;
    }

    ProducesAnnotatedClass producesAnnotatedClass = new ProducesAnnotatedClass(classElement);
    if (!producerClassMap.containsKey(producesAnnotatedClass.getQualifiedClassName())) {
      error(classElement, "@Produced value %s is not supported", producesAnnotatedClass.getSimpleClassName());
      return false;
    }

    String interfaceQualifiedName = producerClassMap.get(producesAnnotatedClass.getQualifiedClassName());
    TypeElement interfaceTypeElement = processingEnv.getElementUtils().getTypeElement(interfaceQualifiedName);
    TypeMirror interfaceTypeMirror = interfaceTypeElement.asType();
    if (!processingEnv.getTypeUtils().isAssignable(
            producesAnnotatedClass.getAnnotatedClassElement().asType(), interfaceTypeMirror)) {
      error(classElement, "The class %s does not implement %s which is required to produce %s",
          classElement.getSimpleName().toString(), interfaceTypeElement.getSimpleName().toString(),
          producesAnnotatedClass.getSimpleClassName());
      return false;
    }

    return true;
  }

  private void error(Element e, String msg, Object... args) {
    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format(msg, args), e);
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    Set<String> annotations = new LinkedHashSet<>();
    annotations.add(Produces.class.getCanonicalName());
    return annotations;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }
}

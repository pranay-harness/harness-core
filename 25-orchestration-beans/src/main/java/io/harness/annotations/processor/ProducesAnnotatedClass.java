package io.harness.annotations.processor;

import io.harness.annotations.Produces;
import lombok.Value;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;

@Value
public class ProducesAnnotatedClass {
  TypeElement annotatedClassElement;
  String qualifiedClassName;
  String simpleClassName;

  public ProducesAnnotatedClass(TypeElement annotatedClassElement) {
    this.annotatedClassElement = annotatedClassElement;
    Produces annotation = annotatedClassElement.getAnnotation(Produces.class);

    String qualifiedClassNameLocal;
    String simpleClassNameLocal;
    try {
      Class<?> clazz = annotation.value();
      qualifiedClassNameLocal = clazz.getCanonicalName();
      simpleClassNameLocal = clazz.getSimpleName();
    } catch (MirroredTypeException mte) {
      // This might happen if the class has not been compiled yet.
      DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
      TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
      qualifiedClassNameLocal = classTypeElement.getQualifiedName().toString();
      simpleClassNameLocal = classTypeElement.getSimpleName().toString();
    }

    this.qualifiedClassName = qualifiedClassNameLocal;
    this.simpleClassName = simpleClassNameLocal;
  }
}

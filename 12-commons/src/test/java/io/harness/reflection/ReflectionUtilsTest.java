package io.harness.reflection;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class ReflectionUtilsTest {
  private class DummyBase { public String baseField; }

  private class Dummy extends DummyBase {
    public String field;
    public String inheritField;
    @DummyAnnotation public String annotatedField;
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  public @interface DummyAnnotation {
    boolean value() default true;
  }

  @Test
  public void getFieldByNameTest() {
    assertThat(ReflectionUtils.getFieldByName(Dummy.class, "dummy")).isNull();
    assertThat(ReflectionUtils.getFieldByName(Dummy.class, "inheritField").getName()).isEqualTo("inheritField");
    assertThat(ReflectionUtils.getFieldByName(Dummy.class, "baseField").getName()).isEqualTo("baseField");
    assertThat(ReflectionUtils.getFieldByName(Dummy.class, "annotatedField").getName()).isEqualTo("annotatedField");
  }

  @Test
  public void getAllDeclaredAndInheritedFields() {
    assertThat(ReflectionUtils.getAllDeclaredAndInheritedFields(Dummy.class)
                   .stream()
                   .filter(f -> f.getName().contains("ield"))
                   .count())
        .isEqualTo(4);
  }

  @Test
  public void getDeclaredAndInheritedFields() {
    assertThat(ReflectionUtils.getDeclaredAndInheritedFields(Dummy.class, f -> f.getName().endsWith("Field")))
        .hasSize(3);
  }

  @Test
  public void updateField() {
    Dummy dummy = new Dummy();
    dummy.annotatedField = "test";
    ReflectionUtils.updateFieldValues(
        dummy, f -> f.isAnnotationPresent(DummyAnnotation.class), value -> value + " hello world");
    assertThat(dummy.annotatedField).isEqualTo("test hello world");
  }
  }

package io.harness.reflection;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReflectionUtilsTest {
  private class FieldBase { public String baseField; }

  private class Field extends FieldBase {
    public String field;
    public String inheritField;
    @DummyAnnotation public String annotatedField;
    @DummyAnnotation public List<String> annotatedListField;
  }

  private class AccessorsBase {
    private String baseAccessor;
    public String getBaseAccessor() {
      return baseAccessor;
    }
  }

  private class Accessors extends AccessorsBase {
    private String privateMethod;
    private String getPrivateMethod() {
      return privateMethod;
    }

    private String protectedMethod;
    protected String getProtectedMethod() {
      return privateMethod;
    }

    public String get() {
      return null;
    }

    public boolean is() {
      return false;
    }

    public String getNoField() {
      return null;
    }

    private String anyType;
    public String getAnyType() {
      return anyType;
    }

    public String isAnyType() {
      return anyType;
    }

    private boolean booleanType;
    public boolean isBooleanType() {
      return booleanType;
    }

    public boolean getBooleanType() {
      return booleanType;
    }

    private Boolean aBoolean;
    public Boolean isABoolean() {
      return aBoolean;
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  public @interface DummyAnnotation {
    boolean value() default true;
  }

  @Test
  @Category(UnitTests.class)
  public void getFieldByNameTest() {
    assertThat(ReflectionUtils.getFieldByName(Field.class, "dummy")).isNull();
    assertThat(ReflectionUtils.getFieldByName(Field.class, "inheritField").getName()).isEqualTo("inheritField");
    assertThat(ReflectionUtils.getFieldByName(Field.class, "baseField").getName()).isEqualTo("baseField");
    assertThat(ReflectionUtils.getFieldByName(Field.class, "annotatedField").getName()).isEqualTo("annotatedField");
    assertThat(ReflectionUtils.getFieldByName(Field.class, "annotatedListField").getName())
        .isEqualTo("annotatedListField");
  }

  @Test
  @Category(UnitTests.class)
  public void getAllDeclaredAndInheritedFields() {
    assertThat(ReflectionUtils.getAllDeclaredAndInheritedFields(Field.class)
                   .stream()
                   .filter(f -> f.getName().contains("ield"))
                   .count())
        .isEqualTo(5);
  }

  @Test
  @Category(UnitTests.class)
  public void getDeclaredAndInheritedFields() {
    assertThat(ReflectionUtils.getDeclaredAndInheritedFields(Field.class, f -> f.getName().endsWith("Field")))
        .hasSize(4);
  }

  @Test
  @Category(UnitTests.class)
  public void updateField() {
    Field dummy = new Field();
    dummy.annotatedField = "test";
    ReflectionUtils.updateFieldValues(
        dummy, f -> f.isAnnotationPresent(DummyAnnotation.class), value -> value + " hello world");
    assertThat(dummy.annotatedField).isEqualTo("test hello world");
  }

  @Test
  @Category(UnitTests.class)
  public void updateListField() {
    Field dummy = new Field();
    List<String> a = new ArrayList<>();
    a.add("one");
    a.add("two");
    dummy.annotatedListField = a;
    ReflectionUtils.updateFieldValues(
        dummy, f -> f.isAnnotationPresent(DummyAnnotation.class), value -> value + " hello world");
    assertThat(dummy.annotatedListField).isEqualTo(ImmutableList.of("one hello world", "two hello world"));
  }

  @Test
  @Category(UnitTests.class)
  public void testGetAccessorMethods() {
    final List<String> accessorMethods = ReflectionUtils.getAccessorMethods(Accessors.class)
                                             .stream()
                                             .map(method -> method.getName())
                                             .collect(Collectors.toList());
    accessorMethods.sort(String::compareTo);

    assertThat(accessorMethods).isEqualTo(asList("getAnyType", "getBaseAccessor", "isABoolean", "isBooleanType"));
  }
  }
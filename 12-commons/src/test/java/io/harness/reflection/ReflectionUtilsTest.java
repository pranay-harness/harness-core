package io.harness.reflection;

import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.PUNEET;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import lombok.Builder;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReflectionUtilsTest extends CategoryTest {
  private static class FieldBase { public String baseField; }

  @Builder
  private static class Field extends FieldBase {
    public String field;
    public String inheritField;
    @DummyAnnotation public String annotatedField;
    @DummyAnnotation public List<String> annotatedListField;
  }

  private static class AccessorsBase {
    private String baseAccessor = "";
    public String getBaseAccessor() {
      return baseAccessor;
    }
  }

  private static class Accessors extends AccessorsBase {
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
  @Owner(developers = PUNEET)
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
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void getAllDeclaredAndInheritedFields() {
    assertThat(ReflectionUtils.getAllDeclaredAndInheritedFields(Field.class)
                   .stream()
                   .filter(f -> f.getName().contains("ield"))
                   .count())
        .isEqualTo(5);
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void getDeclaredAndInheritedFields() {
    assertThat(ReflectionUtils.getDeclaredAndInheritedFields(Field.class, f -> f.getName().endsWith("Field")))
        .hasSize(4);
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void updateField() {
    Field dummy = Field.builder().build();
    dummy.annotatedField = "test";
    ReflectionUtils.updateFieldValues(
        dummy, f -> f.isAnnotationPresent(DummyAnnotation.class), value -> value + " hello world");
    assertThat(dummy.annotatedField).isEqualTo("test hello world");
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void updateListField() {
    Field dummy = Field.builder().build();
    List<String> a = new ArrayList<>();
    a.add("one");
    a.add("two");
    dummy.annotatedListField = a;
    ReflectionUtils.updateFieldValues(
        dummy, f -> f.isAnnotationPresent(DummyAnnotation.class), value -> value + " hello world");
    assertThat(dummy.annotatedListField).isEqualTo(ImmutableList.of("one hello world", "two hello world"));
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testGetAccessorMethods() {
    final List<String> accessorMethods = ReflectionUtils.getAccessorMethods(Accessors.class)
                                             .stream()
                                             .map(method -> method.getName())
                                             .collect(Collectors.toList());
    accessorMethods.sort(String::compareTo);

    assertThat(accessorMethods).isEqualTo(asList("getAnyType", "getBaseAccessor", "isABoolean", "isBooleanType"));
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldGetFieldValues() {
    Field dummy = Field.builder().field("val1").annotatedField("val2").build();
    HashSet<String> fields = new HashSet<>(Arrays.asList("field", "annotatedField", "wrongField"));

    Map<String, Object> fieldValues = ReflectionUtils.getFieldValues(dummy, fields);

    assertThat(fieldValues).hasSize(2);
    assertThat(fieldValues.get("field")).isEqualTo("val1");
    assertThat(fieldValues.get("annotatedField")).isEqualTo("val2");
  }
  }
package io.harness.reflection;

import static io.harness.rule.OwnerRule.AADITI;
import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.PUNEET;
import static io.harness.rule.OwnerRule.UTKARSH;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  @Owner(developers = GEORGE)
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
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void getAllDeclaredAndInheritedFields() {
    assertThat(ReflectionUtils.getAllDeclaredAndInheritedFields(Field.class)
                   .stream()
                   .filter(f -> f.getName().contains("ield"))
                   .count())
        .isEqualTo(5);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void getDeclaredAndInheritedFields() {
    assertThat(ReflectionUtils.getDeclaredAndInheritedFields(Field.class, f -> f.getName().endsWith("Field")))
        .hasSize(4);
  }

  @Test
  @Owner(developers = AADITI)
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

  @Data
  @Builder
  private static class DummyObj {
    List<Pair<String, String>> pairs;
    Map<String, Object> map;
    Set<String> set;
    int intVal;
    String strVal;
    @DummyAnnotation String strValIgnored;
    Object obj;
    String[][] strArrArr;
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testUpdateStringFieldValues() {
    Object obj = ReflectionUtils.updateStrings(null, f -> false, str -> str);
    assertThat(obj).isNull();

    String original = "original";
    String updated = "updated";
    List<Pair<String, String>> pairs =
        asList(ImmutablePair.of(original, original), ImmutablePair.of(original, original));
    Map<String, Object> map = ImmutableMap.of("a", original, "b", 2, "c", ImmutablePair.of(1, original));
    Set<String> set = ImmutableSet.of("a", original);
    DummyObj dummyObjInternal = DummyObj.builder().strVal(original).strValIgnored(original).build();
    String[][] strArrArr = new String[][] {new String[] {"a", original, "b"}, new String[] {"c", original, original}};
    DummyObj dummyObj = DummyObj.builder()
                            .pairs(pairs)
                            .map(map)
                            .set(set)
                            .intVal(5)
                            .strVal(original)
                            .strValIgnored(original)
                            .obj(dummyObjInternal)
                            .strArrArr(strArrArr)
                            .build();
    dummyObjInternal.setObj(dummyObj);

    ReflectionUtils.updateStrings(
        dummyObj, f -> f.isAnnotationPresent(DummyAnnotation.class), str -> str.equals(original) ? updated : str);
    assertThat(pairs.get(0).getLeft()).isEqualTo(updated);
    assertThat(pairs.get(0).getRight()).isEqualTo(updated);
    assertThat(pairs.get(1).getLeft()).isEqualTo(updated);
    assertThat(pairs.get(1).getRight()).isEqualTo(updated);
    assertThat(map.get("a")).isEqualTo(updated);
    assertThat(map.get("b")).isEqualTo(2);
    assertThat(set).containsExactlyInAnyOrder("a", updated);
    assertThat(((Pair<Integer, String>) map.get("c")).getLeft()).isEqualTo(1);
    assertThat(((Pair<Integer, String>) map.get("c")).getRight()).isEqualTo(updated);
    assertThat(dummyObj.getStrVal()).isEqualTo(updated);
    assertThat(dummyObj.getStrValIgnored()).isEqualTo(original);
    assertThat(dummyObj.getObj()).isNotNull();
    assertThat(dummyObjInternal.getPairs()).isNull();
    assertThat(dummyObjInternal.getMap()).isNull();
    assertThat(dummyObjInternal.getIntVal()).isEqualTo(0);
    assertThat(dummyObjInternal.getStrVal()).isEqualTo(updated);
    assertThat(dummyObjInternal.getStrValIgnored()).isEqualTo(original);
    assertThat(dummyObjInternal.getObj()).isEqualTo(dummyObj);
    assertThat(dummyObjInternal.getStrArrArr()).isNull();
    assertThat(strArrArr[0][0]).isEqualTo("a");
    assertThat(strArrArr[0][1]).isEqualTo(updated);
    assertThat(strArrArr[0][2]).isEqualTo("b");
    assertThat(strArrArr[1][0]).isEqualTo("c");
    assertThat(strArrArr[1][1]).isEqualTo(updated);
    assertThat(strArrArr[1][2]).isEqualTo(updated);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testGetAccessorMethods() {
    final List<String> accessorMethods =
        ReflectionUtils.getAccessorMethods(Accessors.class).stream().map(Method::getName).collect(Collectors.toList());
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

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void shouldGetFieldValue() {
    Field dummy = Field.builder().field("val1").annotatedField("val2").build();
    java.lang.reflect.Field dummyField = ReflectionUtils.getFieldByName(dummy.getClass(), "field");
    Object object = ReflectionUtils.getFieldValue(dummy, dummyField);
    assertThat(object).isNotNull();
    assertThat((String) object).isEqualTo("val1");
  }
  }
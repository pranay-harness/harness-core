package io.harness.pms.sdk.core.recast;

import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.pms.sdk.core.PmsSdkCoreTestBase;
import io.harness.pms.serializer.json.JsonOrchestrationUtils;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.bson.Document;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class RecasterTest extends PmsSdkCoreTestBase {
  private final Recaster recaster = new Recaster();

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithLong() {
    final Long longClass = 10L;
    final long longPrimitive = 10;
    new Recast(recaster, ImmutableSet.of(DummyLong.class));
    DummyLong dummyLong = DummyLong.builder().longClass(longClass).longPrimitive(longPrimitive).build();

    Document document = recaster.toDocument(dummyLong);
    assertThat(document).isNotEmpty();
    assertThat(document.get(DummyLong.DummyLongNameConstants.longClass)).isEqualTo(longClass);
    assertThat(document.get(DummyLong.DummyLongNameConstants.longPrimitive)).isEqualTo(longPrimitive);

    DummyLong recastedDummyLong = recaster.fromDocument(document, DummyLong.class);
    assertThat(recastedDummyLong).isNotNull();
    assertThat(recastedDummyLong.longClass).isEqualTo(longClass);
    assertThat(recastedDummyLong.longPrimitive).isEqualTo(longPrimitive);
  }

  @Builder
  @FieldNameConstants(innerTypeName = "DummyLongNameConstants")
  @NoArgsConstructor
  @AllArgsConstructor
  private static class DummyLong {
    private Long longClass;
    private long longPrimitive;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithInteger() {
    final Integer integerClass = 10;
    final int intPrimitive = 10;
    new Recast(recaster, ImmutableSet.of(DummyInteger.class));
    DummyInteger dummyInteger = DummyInteger.builder().integerClass(integerClass).intPrimitive(intPrimitive).build();

    Document document = recaster.toDocument(dummyInteger);
    assertThat(document).isNotEmpty();
    assertThat(document.get(DummyInteger.DummyIntegerNameConstants.integerClass)).isEqualTo(integerClass);
    assertThat(document.get(DummyInteger.DummyIntegerNameConstants.intPrimitive)).isEqualTo(intPrimitive);

    DummyInteger recastedDummyInteger = recaster.fromDocument(document, DummyInteger.class);
    assertThat(recastedDummyInteger).isNotNull();
    assertThat(recastedDummyInteger.integerClass).isEqualTo(integerClass);
    assertThat(recastedDummyInteger.intPrimitive).isEqualTo(intPrimitive);
  }

  @Builder
  @FieldNameConstants(innerTypeName = "DummyIntegerNameConstants")
  @NoArgsConstructor
  @AllArgsConstructor
  private static class DummyInteger {
    private Integer integerClass;
    private int intPrimitive;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithDouble() {
    final Double doubleClass = 10.0;
    final double doublePrimitive = 10.0;
    new Recast(recaster, ImmutableSet.of(DummyDouble.class));
    DummyDouble dummyLong = DummyDouble.builder().doubleClass(doubleClass).doublePrimitive(doublePrimitive).build();

    Document document = recaster.toDocument(dummyLong);
    assertThat(document).isNotEmpty();
    assertThat(document.get(DummyDouble.DummyDoubleNameConstants.doubleClass)).isEqualTo(doubleClass);
    assertThat(document.get(DummyDouble.DummyDoubleNameConstants.doublePrimitive)).isEqualTo(doublePrimitive);

    DummyDouble recastedDummyDouble = recaster.fromDocument(document, DummyDouble.class);
    assertThat(recastedDummyDouble).isNotNull();
    assertThat(recastedDummyDouble.doubleClass).isEqualTo(doubleClass);
    assertThat(recastedDummyDouble.doubleClass).isEqualTo(doubleClass);
  }

  @Builder
  @FieldNameConstants(innerTypeName = "DummyDoubleNameConstants")
  @NoArgsConstructor
  @AllArgsConstructor
  private static class DummyDouble {
    private Double doubleClass;
    private double doublePrimitive;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithFloat() {
    final Float floatClass = 10.0f;
    final float floatPrimitive = 10.0f;
    new Recast(recaster, ImmutableSet.of(DummyFloat.class));
    DummyFloat dummyFloat = DummyFloat.builder().floatClass(floatClass).floatPrimitive(floatPrimitive).build();

    Document document = recaster.toDocument(dummyFloat);
    assertThat(document).isNotEmpty();
    assertThat(document.get(DummyFloat.DummyFloatNameConstants.floatClass)).isEqualTo(floatClass);
    assertThat(document.get(DummyFloat.DummyFloatNameConstants.floatPrimitive)).isEqualTo(floatPrimitive);

    DummyFloat recastedDummyFloat = recaster.fromDocument(document, DummyFloat.class);
    assertThat(recastedDummyFloat).isNotNull();
    assertThat(recastedDummyFloat.floatClass).isEqualTo(floatClass);
    assertThat(recastedDummyFloat.floatPrimitive).isEqualTo(floatPrimitive);
  }

  @Builder
  @FieldNameConstants(innerTypeName = "DummyFloatNameConstants")
  @NoArgsConstructor
  @AllArgsConstructor
  private static class DummyFloat {
    private Float floatClass;
    private float floatPrimitive;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithCharacter() {
    final Character characterClass = 'A';
    final char charPrimitive = 'a';
    new Recast(recaster, ImmutableSet.of(DummyCharacter.class));
    DummyCharacter dummyCharacter =
        DummyCharacter.builder().characterClass(characterClass).charPrimitive(charPrimitive).build();

    Document document = recaster.toDocument(dummyCharacter);
    assertThat(document).isNotEmpty();
    assertThat(document.get(DummyCharacter.DummyCharacterNameConstants.characterClass)).isEqualTo(characterClass);
    assertThat(document.get(DummyCharacter.DummyCharacterNameConstants.charPrimitive)).isEqualTo(charPrimitive);

    DummyCharacter recastedDummyCharacter = recaster.fromDocument(document, DummyCharacter.class);
    assertThat(recastedDummyCharacter).isNotNull();
    assertThat(recastedDummyCharacter.characterClass).isEqualTo(characterClass);
    assertThat(recastedDummyCharacter.charPrimitive).isEqualTo(charPrimitive);
  }

  @Builder
  @FieldNameConstants(innerTypeName = "DummyCharacterNameConstants")
  @NoArgsConstructor
  @AllArgsConstructor
  private static class DummyCharacter {
    private Character characterClass;
    private char charPrimitive;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithBoolean() {
    final Boolean booleanClass = true;
    final boolean booleanPrimitive = false;
    new Recast(recaster, ImmutableSet.of(DummyBoolean.class));
    DummyBoolean dummyBoolean =
        DummyBoolean.builder().booleanClass(booleanClass).booleanPrimitive(booleanPrimitive).build();

    Document document = recaster.toDocument(dummyBoolean);
    assertThat(document).isNotEmpty();
    assertThat(document.get(DummyBoolean.DummyBooleanNameConstants.booleanClass)).isEqualTo(booleanClass);
    assertThat(document.get(DummyBoolean.DummyBooleanNameConstants.booleanPrimitive)).isEqualTo(booleanPrimitive);

    DummyBoolean recastedDummyBoolean = recaster.fromDocument(document, DummyBoolean.class);
    assertThat(recastedDummyBoolean).isNotNull();
    assertThat(recastedDummyBoolean.booleanClass).isEqualTo(booleanClass);
    assertThat(recastedDummyBoolean.booleanPrimitive).isEqualTo(booleanPrimitive);
  }

  @Builder
  @FieldNameConstants(innerTypeName = "DummyBooleanNameConstants")
  @NoArgsConstructor
  @AllArgsConstructor
  private static class DummyBoolean {
    private Boolean booleanClass;
    private boolean booleanPrimitive;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithString() {
    final String stringClass = "sdgg";
    new Recast(recaster, ImmutableSet.of(DummyString.class));
    DummyString dummyBoolean = DummyString.builder().stringClass(stringClass).build();

    Document document = recaster.toDocument(dummyBoolean);
    assertThat(document).isNotEmpty();
    assertThat(document.get(DummyString.DummyStringNameConstants.stringClass)).isEqualTo(stringClass);

    DummyString recastedDummyString = recaster.fromDocument(document, DummyString.class);
    assertThat(recastedDummyString).isNotNull();
    assertThat(recastedDummyString.stringClass).isEqualTo(stringClass);
  }

  @Builder
  @FieldNameConstants(innerTypeName = "DummyStringNameConstants")
  @NoArgsConstructor
  @AllArgsConstructor
  private static class DummyString {
    private String stringClass;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithEnum() {
    new Recast(recaster, ImmutableSet.of(DummyEnum.class));
    DummyEnum dummyEnum = DummyEnum.builder().type(DummyEnum.Type.SUPER_DUMMY).build();

    Document document = recaster.toDocument(dummyEnum);
    assertThat(document).isNotEmpty();
    assertThat(document.get(DummyEnum.DummyEnumNameConstants.type)).isEqualTo(DummyEnum.Type.SUPER_DUMMY.name());

    DummyEnum recastedDummyEnum = recaster.fromDocument(document, DummyEnum.class);
    assertThat(recastedDummyEnum).isNotNull();
    assertThat(recastedDummyEnum.type).isEqualTo(DummyEnum.Type.SUPER_DUMMY);
  }

  @Builder
  @FieldNameConstants(innerTypeName = "DummyEnumNameConstants")
  @NoArgsConstructor
  @AllArgsConstructor
  private static class DummyEnum {
    private Type type;
    private enum Type { SUPER_DUMMY }
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithStringKeyMap() {
    Map<String, String> map = new HashMap<>();
    map.put("Test", "Success");
    map.put("Test1", "Success");
    new Recast(recaster, ImmutableSet.of(DummyStringKeyMap.class));
    DummyStringKeyMap stringKeyMap = DummyStringKeyMap.builder().map(map).build();

    Document document = recaster.toDocument(stringKeyMap);
    assertThat(document).isNotEmpty();
    assertThat(((Document) document.get(DummyStringKeyMap.DummyStringKeyMapNameConstants.map)))
        .isEqualTo(Document.parse(JsonOrchestrationUtils.asJson(map)));

    DummyStringKeyMap recastedDummyMap = recaster.fromDocument(document, DummyStringKeyMap.class);
    assertThat(recastedDummyMap).isNotNull();
    assertThat(recastedDummyMap.map).isEqualTo(map);
  }

  @Builder
  @FieldNameConstants(innerTypeName = "DummyStringKeyMapNameConstants")
  @NoArgsConstructor
  @AllArgsConstructor
  private static class DummyStringKeyMap {
    private Map<String, String> map;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithBasicCharacterArray() {
    final Character[] characterClassArray = new Character[] {'A', 'B'};
    final char[] charPrimitiveArray = new char[] {'a', 'b'};

    new Recast(recaster, ImmutableSet.of(DummyCharacterArray.class));
    DummyCharacterArray user = DummyCharacterArray.builder()
                                   .characterClassArray(characterClassArray)
                                   .charPrimitiveArray(charPrimitiveArray)
                                   .build();

    Document document = recaster.toDocument(user);
    assertThat(document).isNotEmpty();
    assertThat(document.get(DummyCharacterArray.DummyCharacterArrayConstants.characterClassArray))
        .isEqualTo(characterClassArray);
    assertThat(document.get(DummyCharacterArray.DummyCharacterArrayConstants.charPrimitiveArray))
        .isEqualTo(charPrimitiveArray);

    DummyCharacterArray recastedDummyCharacterArray = recaster.fromDocument(document, DummyCharacterArray.class);
    assertThat(recastedDummyCharacterArray).isNotNull();
    assertThat(recastedDummyCharacterArray.characterClassArray).isEqualTo(characterClassArray);
    assertThat(recastedDummyCharacterArray.charPrimitiveArray).isEqualTo(charPrimitiveArray);
  }

  @Builder
  @FieldNameConstants(innerTypeName = "DummyCharacterArrayConstants")
  @NoArgsConstructor
  @AllArgsConstructor
  private static class DummyCharacterArray {
    private Character[] characterClassArray;
    private char[] charPrimitiveArray;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithSimpleList() {
    final List<Integer> list = Arrays.asList(1, 2, 3);

    new Recast(recaster, ImmutableSet.of(DummySimpleList.class));
    DummySimpleList dummyList = DummySimpleList.builder().list(list).build();
    Document document = recaster.toDocument(dummyList);

    assertThat(document).isNotEmpty();
    assertThat(document.get(DummySimpleList.DummySimpleListConstants.list)).isEqualTo(list);

    DummySimpleList recastedSimpleList = recaster.fromDocument(document, DummySimpleList.class);
    assertThat(recastedSimpleList).isNotNull();
    assertThat(recastedSimpleList.list).isEqualTo(list);
  }

  @Builder
  @FieldNameConstants(innerTypeName = "DummySimpleListConstants")
  @NoArgsConstructor
  @AllArgsConstructor
  private static class DummySimpleList {
    private List<Integer> list;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithInnerClass() {
    final String id = "dgrr4kg02-24ger40bdf-4";
    final String name = "sgnjdfg";
    final Integer age = 21;
    DummyWithInnerClass.User user = DummyWithInnerClass.User.builder().name(name).age(age).build();
    new Recast(recaster, ImmutableSet.of(DummyWithInnerClass.class));
    DummyWithInnerClass dummyWithInnerClass = DummyWithInnerClass.builder().id(id).user(user).build();

    Document document = recaster.toDocument(dummyWithInnerClass);
    assertThat(document).isNotEmpty();
    assertThat(document.get(DummyWithInnerClass.DummyWithInnerClassConstants.id)).isEqualTo(id);
    Document userDocument = (Document) document.get(DummyWithInnerClass.DummyWithInnerClassConstants.user);
    assertThat(userDocument).isNotEmpty();
    assertThat(userDocument.get(DummyWithInnerClass.User.DummyWithInnerClassConstants.name)).isEqualTo(name);
    assertThat(userDocument.get(DummyWithInnerClass.User.DummyWithInnerClassConstants.age)).isEqualTo(age);

    DummyWithInnerClass recastedDummyWithInnerClass = recaster.fromDocument(document, DummyWithInnerClass.class);
    assertThat(recastedDummyWithInnerClass).isNotNull();
    assertThat(recastedDummyWithInnerClass.id).isEqualTo(id);
    assertThat(recastedDummyWithInnerClass.user).isEqualTo(user);
  }

  @Builder
  @FieldNameConstants(innerTypeName = "DummyWithInnerClassConstants")
  @NoArgsConstructor
  @AllArgsConstructor
  private static class DummyWithInnerClass {
    private String id;
    private User user;

    @Builder
    @FieldNameConstants(innerTypeName = "DummyWithInnerClassConstants")
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    private static class User {
      private String name;
      private Integer age;
    }
  }
}

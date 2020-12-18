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
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import org.bson.Document;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class RecasterTest extends PmsSdkCoreTestBase {
  private final Recaster recaster = new Recaster();

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestToDocumentWithLong() {
    final Long longClass = 10L;
    final long longPrimitive = 10;
    new Recast(recaster, ImmutableSet.of(DummyLong.class));
    DummyLong dummyLong = DummyLong.builder().longClass(longClass).longPrimitive(longPrimitive).build();

    Document document = recaster.toDocument(dummyLong);

    assertThat(document).isNotEmpty();

    assertThat(document.get(DummyLong.DummyLongNameConstants.longClass)).isEqualTo(longClass);
    assertThat(document.get(DummyLong.DummyLongNameConstants.longPrimitive)).isEqualTo(longPrimitive);
  }

  @Builder
  @FieldNameConstants(innerTypeName = "DummyLongNameConstants")
  private static class DummyLong {
    private final Long longClass;
    private final long longPrimitive;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestToDocumentWithInteger() {
    final Integer integerClass = 10;
    final int intPrimitive = 10;
    new Recast(recaster, ImmutableSet.of(DummyInteger.class));
    DummyInteger dummyLong = DummyInteger.builder().longClass(integerClass).longPrimitive(intPrimitive).build();

    Document document = recaster.toDocument(dummyLong);

    assertThat(document).isNotEmpty();

    assertThat(document.get(DummyInteger.DummyIntegerNameConstants.longClass)).isEqualTo(integerClass);
    assertThat(document.get(DummyInteger.DummyIntegerNameConstants.longPrimitive)).isEqualTo(intPrimitive);
  }

  @Builder
  @FieldNameConstants(innerTypeName = "DummyIntegerNameConstants")
  private static class DummyInteger {
    private final Integer longClass;
    private final int longPrimitive;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestToDocumentWithDouble() {
    final Double doubleClass = 10.0;
    final double doublePrimitive = 10.0;
    new Recast(recaster, ImmutableSet.of(DummyDouble.class));
    DummyDouble dummyLong = DummyDouble.builder().doubleClass(doubleClass).doublePrimitive(doublePrimitive).build();

    Document document = recaster.toDocument(dummyLong);

    assertThat(document).isNotEmpty();

    assertThat(document.get(DummyDouble.DummyDoubleNameConstants.doubleClass)).isEqualTo(doubleClass);
    assertThat(document.get(DummyDouble.DummyDoubleNameConstants.doublePrimitive)).isEqualTo(doublePrimitive);
  }

  @Builder
  @FieldNameConstants(innerTypeName = "DummyDoubleNameConstants")
  private static class DummyDouble {
    private final Double doubleClass;
    private final double doublePrimitive;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestToDocumentWithFloat() {
    final Float floatClass = 10.0f;
    final float floatPrimitive = 10.0f;
    new Recast(recaster, ImmutableSet.of(DummyFloat.class));
    DummyFloat dummyFloat = DummyFloat.builder().floatClass(floatClass).floatPrimitive(floatPrimitive).build();

    Document document = recaster.toDocument(dummyFloat);

    assertThat(document).isNotEmpty();

    assertThat(document.get(DummyFloat.DummyFloatNameConstants.floatClass)).isEqualTo(floatClass);
    assertThat(document.get(DummyFloat.DummyFloatNameConstants.floatPrimitive)).isEqualTo(floatPrimitive);
  }

  @Builder
  @FieldNameConstants(innerTypeName = "DummyFloatNameConstants")
  private static class DummyFloat {
    private final Float floatClass;
    private final float floatPrimitive;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestToDocumentWithCharacter() {
    final Character characterClass = 'A';
    final char charPrimitive = 'a';
    new Recast(recaster, ImmutableSet.of(DummyCharacter.class));
    DummyCharacter dummyCharacter =
        DummyCharacter.builder().characterClass(characterClass).charPrimitive(charPrimitive).build();

    Document document = recaster.toDocument(dummyCharacter);

    assertThat(document).isNotEmpty();

    assertThat(document.get(DummyCharacter.DummyCharacterNameConstants.characterClass)).isEqualTo(characterClass);
    assertThat(document.get(DummyCharacter.DummyCharacterNameConstants.charPrimitive)).isEqualTo(charPrimitive);
  }

  @Builder
  @FieldNameConstants(innerTypeName = "DummyCharacterNameConstants")
  private static class DummyCharacter {
    private final Character characterClass;
    private final char charPrimitive;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestToDocumentWithBoolean() {
    final Boolean booleanClass = true;
    final boolean booleanPrimitive = false;
    new Recast(recaster, ImmutableSet.of(DummyBoolean.class));
    DummyBoolean dummyBoolean =
        DummyBoolean.builder().booleanClass(booleanClass).booleanPrimitive(booleanPrimitive).build();

    Document document = recaster.toDocument(dummyBoolean);

    assertThat(document).isNotEmpty();

    assertThat(document.get(DummyBoolean.DummyBooleanNameConstants.booleanClass)).isEqualTo(booleanClass);
    assertThat(document.get(DummyBoolean.DummyBooleanNameConstants.booleanPrimitive)).isEqualTo(booleanPrimitive);
  }

  @Builder
  @FieldNameConstants(innerTypeName = "DummyBooleanNameConstants")
  private static class DummyBoolean {
    private final Boolean booleanClass;
    private final boolean booleanPrimitive;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestToDocumentWithString() {
    final String stringClass = "sdgg";
    new Recast(recaster, ImmutableSet.of(DummyString.class));
    DummyString dummyBoolean = DummyString.builder().stringClass(stringClass).build();

    Document document = recaster.toDocument(dummyBoolean);

    assertThat(document).isNotEmpty();

    assertThat(document.get(DummyString.DummyStringNameConstants.stringClass)).isEqualTo(stringClass);
  }

  @Builder
  @FieldNameConstants(innerTypeName = "DummyStringNameConstants")
  private static class DummyString {
    private final String stringClass;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestToDocumentWithEnum() {
    new Recast(recaster, ImmutableSet.of(DummyEnum.class));
    DummyEnum dummyEnum = DummyEnum.builder().type(DummyEnum.Type.SUPER_DUMMY).build();

    Document document = recaster.toDocument(dummyEnum);

    assertThat(document).isNotEmpty();

    assertThat(((Document) document.get(DummyEnum.DummyEnumNameConstants.type)).get("name"))
        .isEqualTo(DummyEnum.Type.SUPER_DUMMY.name());
  }

  @Builder
  @FieldNameConstants(innerTypeName = "DummyEnumNameConstants")
  private static class DummyEnum {
    private final Type type;
    private enum Type { SUPER_DUMMY }
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestToDocumentWithStringKeyMap() {
    Map<String, String> map = new HashMap<>();
    map.put("Test", "Success");
    map.put("Test1", "Success");
    new Recast(recaster, ImmutableSet.of(DummyStringKeyMap.class));
    DummyStringKeyMap stringKeyMap = DummyStringKeyMap.builder().map(map).build();

    Document document = recaster.toDocument(stringKeyMap);

    assertThat(document).isNotEmpty();

    assertThat(((Document) document.get(DummyStringKeyMap.DummyStringKeyMapNameConstants.map)))
        .isEqualTo(Document.parse(JsonOrchestrationUtils.asJson(map)));
  }

  @Builder
  @FieldNameConstants(innerTypeName = "DummyStringKeyMapNameConstants")
  private static class DummyStringKeyMap {
    private final Map<String, String> map;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @Ignore("Complex types are not supported for multivalue")
  public void shouldTestToDocumentWithBasicCharacterArray() {
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
  }

  @Builder
  @FieldNameConstants(innerTypeName = "DummyCharacterArrayConstants")
  private static class DummyCharacterArray {
    private final Character[] characterClassArray;
    private final char[] charPrimitiveArray;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestToDocumentWithSimpleList() {
    final List<Integer> list = Arrays.asList(1, 2, 3);

    new Recast(recaster, ImmutableSet.of(DummySimpleList.class));
    DummySimpleList dummyList = DummySimpleList.builder().list(list).build();
    Document document = recaster.toDocument(dummyList);

    assertThat(document).isNotEmpty();

    assertThat(document.get(DummySimpleList.DummySimpleListConstants.list)).isEqualTo(list);
  }

  @Builder
  @FieldNameConstants(innerTypeName = "DummySimpleListConstants")
  private static class DummySimpleList {
    private final List<Integer> list;
  }
}

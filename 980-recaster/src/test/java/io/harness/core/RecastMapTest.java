/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.core;

import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.RecasterTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exceptions.RecasterException;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class RecastMapTest extends RecasterTestBase {
  private Recaster recaster;

  @Before
  public void setup() {
    recaster = new Recaster(RecasterOptions.builder().workWithMaps(true).build());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithNull() {
    Recast recast = new Recast(recaster, ImmutableSet.of());
    DummyLong recastedDummyLong = recast.fromMap(null, DummyLong.class);
    assertThat(recastedDummyLong).isNull();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithEmptyDocument() {
    Recast recast = new Recast(recaster, ImmutableSet.of());
    assertThatThrownBy(() -> recast.fromMap(new HashMap<>(), DummyLong.class))
        .isInstanceOf(RecasterException.class)
        .hasMessageContaining(
            "The document does not contain any identifiers __recast. Determining entity type is impossible. Consider adding RecasterAlias annotation to your class");
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithLong() {
    final Long longClass = 10L;
    final long longPrimitive = 10;
    Recast recast = new Recast(recaster, ImmutableSet.of(DummyLong.class));
    DummyLong dummyLong = DummyLong.builder().longClass(longClass).longPrimitive(longPrimitive).build();

    Map<String, Object> document = recast.toMap(dummyLong);
    assertThat(document).isNotEmpty();
    assertThat(document.get("longClass")).isEqualTo(longClass);
    assertThat(document.get("longPrimitive")).isEqualTo(longPrimitive);

    DummyLong recastedDummyLong = recast.fromMap(document, DummyLong.class);
    assertThat(recastedDummyLong).isNotNull();
    assertThat(recastedDummyLong.longClass).isEqualTo(longClass);
    assertThat(recastedDummyLong.longPrimitive).isEqualTo(longPrimitive);
  }

  @Builder
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
    Recast recast = new Recast(recaster, ImmutableSet.of(DummyInteger.class));
    DummyInteger dummyInteger = DummyInteger.builder().integerClass(integerClass).intPrimitive(intPrimitive).build();

    Map<String, Object> document = recast.toMap(dummyInteger);
    assertThat(document).isNotEmpty();
    assertThat(document.get("integerClass")).isEqualTo(integerClass);
    assertThat(document.get("intPrimitive")).isEqualTo(intPrimitive);

    DummyInteger recastedDummyInteger = recast.fromMap(document, DummyInteger.class);
    assertThat(recastedDummyInteger).isNotNull();
    assertThat(recastedDummyInteger.integerClass).isEqualTo(integerClass);
    assertThat(recastedDummyInteger.intPrimitive).isEqualTo(intPrimitive);
  }

  @Builder
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
    Recast recast = new Recast(recaster, ImmutableSet.of(DummyDouble.class));
    DummyDouble dummyLong = DummyDouble.builder().doubleClass(doubleClass).doublePrimitive(doublePrimitive).build();

    Map<String, Object> document = recast.toMap(dummyLong);
    assertThat(document).isNotEmpty();
    assertThat(document.get("doubleClass")).isEqualTo(doubleClass);
    assertThat(document.get("doublePrimitive")).isEqualTo(doublePrimitive);

    DummyDouble recastedDummyDouble = recast.fromMap(document, DummyDouble.class);
    assertThat(recastedDummyDouble).isNotNull();
    assertThat(recastedDummyDouble.doubleClass).isEqualTo(doubleClass);
    assertThat(recastedDummyDouble.doubleClass).isEqualTo(doubleClass);
  }

  @Builder
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
    Recast recast = new Recast(recaster, ImmutableSet.of(DummyFloat.class));
    DummyFloat dummyFloat = DummyFloat.builder().floatClass(floatClass).floatPrimitive(floatPrimitive).build();

    Map<String, Object> document = recast.toMap(dummyFloat);
    assertThat(document).isNotEmpty();
    assertThat(document.get("floatClass")).isEqualTo(floatClass);
    assertThat(document.get("floatPrimitive")).isEqualTo(floatPrimitive);

    DummyFloat recastedDummyFloat = recast.fromMap(document, DummyFloat.class);
    assertThat(recastedDummyFloat).isNotNull();
    assertThat(recastedDummyFloat.floatClass).isEqualTo(floatClass);
    assertThat(recastedDummyFloat.floatPrimitive).isEqualTo(floatPrimitive);
  }

  @Builder
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
    Recast recast = new Recast(recaster, ImmutableSet.of(DummyCharacter.class));
    DummyCharacter dummyCharacter =
        DummyCharacter.builder().characterClass(characterClass).charPrimitive(charPrimitive).build();

    Map<String, Object> document = recast.toMap(dummyCharacter);
    assertThat(document).isNotEmpty();
    assertThat(document.get("characterClass")).isEqualTo(characterClass);
    assertThat(document.get("charPrimitive")).isEqualTo(charPrimitive);

    DummyCharacter recastedDummyCharacter = recast.fromMap(document, DummyCharacter.class);
    assertThat(recastedDummyCharacter).isNotNull();
    assertThat(recastedDummyCharacter.characterClass).isEqualTo(characterClass);
    assertThat(recastedDummyCharacter.charPrimitive).isEqualTo(charPrimitive);
  }

  @Builder
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
    Recast recast = new Recast(recaster, ImmutableSet.of(DummyBoolean.class));
    DummyBoolean dummyBoolean =
        DummyBoolean.builder().booleanClass(booleanClass).booleanPrimitive(booleanPrimitive).build();

    Map<String, Object> document = recast.toMap(dummyBoolean);
    assertThat(document).isNotEmpty();
    assertThat(document.get("booleanClass")).isEqualTo(booleanClass);
    assertThat(document.get("booleanPrimitive")).isEqualTo(booleanPrimitive);

    DummyBoolean recastedDummyBoolean = recast.fromMap(document, DummyBoolean.class);
    assertThat(recastedDummyBoolean).isNotNull();
    assertThat(recastedDummyBoolean.booleanClass).isEqualTo(booleanClass);
    assertThat(recastedDummyBoolean.booleanPrimitive).isEqualTo(booleanPrimitive);
  }

  @Builder
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
    Recast recast = new Recast(recaster, ImmutableSet.of(DummyString.class));
    DummyString dummyBoolean = DummyString.builder().stringClass(stringClass).build();

    Map<String, Object> document = recast.toMap(dummyBoolean);
    assertThat(document).isNotEmpty();
    assertThat(document.get("stringClass")).isEqualTo(stringClass);

    DummyString recastedDummyString = recast.fromMap(document, DummyString.class);
    assertThat(recastedDummyString).isNotNull();
    assertThat(recastedDummyString.stringClass).isEqualTo(stringClass);
  }

  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  private static class DummyString {
    private String stringClass;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithEnum() {
    Recast recast = new Recast(recaster, ImmutableSet.of(DummyEnum.class));
    DummyEnum dummyEnum = DummyEnum.builder().type(DummyEnum.Type.SUPER_DUMMY).build();

    Map<String, Object> document = recast.toMap(dummyEnum);
    assertThat(document).isNotEmpty();
    assertThat(document.get("type")).isEqualTo(DummyEnum.Type.SUPER_DUMMY.name());

    DummyEnum recastedDummyEnum = recast.fromMap(document, DummyEnum.class);
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
    Recast recast = new Recast(recaster, ImmutableSet.of(DummyStringKeyMap.class));
    DummyStringKeyMap stringKeyMap = DummyStringKeyMap.builder().map(map).build();
    Map<String, Object> document = recast.toMap(stringKeyMap);
    assertThat(document).isNotEmpty();
    assertThat((Map<String, Object>) document.get("map"))
        .isEqualTo(ImmutableMap.of("Test", "Success", "Test1", "Success"));

    DummyStringKeyMap recastedDummyMap = recast.fromMap(document, DummyStringKeyMap.class);
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

    Recast recast = new Recast(recaster, ImmutableSet.of(DummyCharacterArray.class));
    DummyCharacterArray user = DummyCharacterArray.builder()
                                   .characterClassArray(characterClassArray)
                                   .charPrimitiveArray(charPrimitiveArray)
                                   .build();

    Map<String, Object> document = recast.toMap(user);
    assertThat(document).isNotEmpty();
    assertThat(document.get("characterClassArray")).isEqualTo("AB");
    assertThat(document.get("charPrimitiveArray")).isEqualTo("ab");
    // assertThat(Map<String, Object>.parse(document.toJson())).isEqualTo(document);

    DummyCharacterArray recastedDummyCharacterArray = recast.fromMap(document, DummyCharacterArray.class);
    assertThat(recastedDummyCharacterArray).isNotNull();
    assertThat(recastedDummyCharacterArray.characterClassArray).isEqualTo(characterClassArray);
    assertThat(recastedDummyCharacterArray.charPrimitiveArray).isEqualTo(charPrimitiveArray);
  }

  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  private static class DummyCharacterArray {
    private Character[] characterClassArray;
    private char[] charPrimitiveArray;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithDate() {
    final Instant instant = Instant.now();

    Recast recast = new Recast(recaster, ImmutableSet.of(DummyDate.class));
    DummyDate dummyDate = DummyDate.builder().date(Date.from(instant)).build();
    Map<String, Object> document = recast.toMap(dummyDate);

    assertThat(document).isNotEmpty();
    assertThat(document.get("date")).isEqualTo(dummyDate.date);

    DummyDate recastedDummyDate = recast.fromMap(document, DummyDate.class);
    assertThat(recastedDummyDate).isNotNull();
    assertThat(recastedDummyDate.date.toInstant().toEpochMilli()).isEqualTo(instant.toEpochMilli());
  }

  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  private static class DummyDate {
    private Date date;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithLocalDate() {
    final LocalDate now = LocalDate.now();

    Recast recast = new Recast(recaster, ImmutableSet.of(DummyLocalDate.class));
    DummyLocalDate dummyLocalDate = DummyLocalDate.builder().localDate(now).build();
    Map<String, Object> document = recast.toMap(dummyLocalDate);

    assertThat(document).isNotEmpty();
    assertThat(document.get("localDate")).isEqualTo(dummyLocalDate.localDate);

    DummyLocalDate recastedDummyLocalDate = recast.fromMap(document, DummyLocalDate.class);
    assertThat(recastedDummyLocalDate).isNotNull();
    assertThat(recastedDummyLocalDate.localDate).isEqualTo(now);
  }

  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  private static class DummyLocalDate {
    private LocalDate localDate;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithLocalDateTime() {
    final LocalDateTime now = LocalDateTime.now();

    Recast recast = new Recast(recaster, ImmutableSet.of(DummyLocalDateTime.class));
    DummyLocalDateTime dummyLocalDateTime = DummyLocalDateTime.builder().localDatetime(now).build();
    Map<String, Object> document = recast.toMap(dummyLocalDateTime);

    assertThat(document).isNotEmpty();
    assertThat(document.get("localDatetime")).isEqualTo(now);

    DummyLocalDateTime recastedDummyLocalDateTime = recast.fromMap(document, DummyLocalDateTime.class);
    assertThat(recastedDummyLocalDateTime).isNotNull();
    assertThat(recastedDummyLocalDateTime.localDatetime).isEqualTo(now);
  }

  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  private static class DummyLocalDateTime {
    private LocalDateTime localDatetime;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithLocalTime() {
    final LocalTime now = LocalTime.now();

    Recast recast = new Recast(recaster, ImmutableSet.of(DummyLocalTime.class));
    DummyLocalTime dummyLocalDateTime = DummyLocalTime.builder().localTime(now).build();
    Map<String, Object> document = recast.toMap(dummyLocalDateTime);

    assertThat(document).isNotEmpty();
    assertThat(document.get("localTime")).isEqualTo(now);

    DummyLocalTime recastedDummyLocalTime = recast.fromMap(document, DummyLocalTime.class);
    assertThat(recastedDummyLocalTime).isNotNull();
    assertThat(recastedDummyLocalTime.localTime).isEqualTo(now);
  }

  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  private static class DummyLocalTime {
    private LocalTime localTime;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithInstant() {
    final Instant instant = Instant.now();

    Recast recast = new Recast(recaster, ImmutableSet.of(DummyInstant.class));
    DummyInstant dummyInstant = DummyInstant.builder().instant(instant).build();
    Map<String, Object> document = recast.toMap(dummyInstant);

    assertThat(document).isNotEmpty();
    assertThat(document.get("instant")).isEqualTo(instant);

    DummyInstant recastedDummyInstant = recast.fromMap(document, DummyInstant.class);
    assertThat(recastedDummyInstant).isNotNull();
    assertThat(recastedDummyInstant.instant).isEqualTo(instant);
  }

  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  private static class DummyInstant {
    private Instant instant;
  }
}

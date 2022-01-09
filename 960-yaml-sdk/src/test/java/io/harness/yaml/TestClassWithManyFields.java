/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.yaml;

import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.list;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.map;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.number;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.DX)
public class TestClassWithManyFields {
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PUBLIC)
  public static class ClassWithoutApiModelOverride1 extends TestAbstractClass {
    String testString;
    @YamlSchemaTypes(value = {list, map}, defaultType = list) TestRandomClass1 testRandomClass1;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PUBLIC)
  public static class ClassWhichContainsInterface1 {
    @NotNull Types1 type;
    @JsonProperty("spec")
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
    TestAbstractClass abstractClass;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PUBLIC)
  @JsonSubTypes(
      { @JsonSubTypes.Type(value = ClassWithoutApiModelOverride1.class, name = "ClassWithoutApiModelOverride1") })
  public static class TestAbstractClass {
    @YamlSchemaTypes({string, number}) TestRandomClass2 abstractClass1;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PUBLIC)
  public static class TestRandomClass1 {
    int testR11;
    int testR12;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PUBLIC)
  public static class TestRandomClass2 {
    int testR21;
    int testR22;
    TestRandomClass1 testRandomClass1;
  }

  public enum Types1 { ClassWithoutApiModelOverride1 }

  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PUBLIC)
  public static class ClassWithoutApiModelOverride3 extends TestAbstractClass {
    String testString;
    @YamlSchemaTypes(value = {list, map}, defaultType = list, pattern = "abc") TestRandomClass1 testRandomClass1;
  }

  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PUBLIC)
  public static class ClassWithoutApiModelOverride4 {
    String testString;
    @YamlSchemaTypes(value = {string, map}, defaultType = string, pattern = "abc", minLength = 1)
    TestRandomClass1 testRandomClass1;
  }

  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PUBLIC)
  public static class ClassWithNonEmptyField {
    @NotEmpty String testString1;
    String testString2;
  }
}

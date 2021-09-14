/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.yaml;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.validation.OneOfField;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@OwnedBy(HarnessTeam.DX)
public class TestClass {
  @JsonSubTypes({
    @JsonSubTypes.Type(value = ClassWithApiModelOverride.class, name = "ClassWithApiModelOverride")
    , @JsonSubTypes.Type(value = ClassWithoutApiModelOverride.class, name = "ClassWithoutApiModelOverride")
  })
  public interface TestInterface {}

  @ApiModel(value = "testName")
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @OneOfField(fields = {"a", "b"})
  @OneOfField(fields = {"c", "d"})
  @FieldDefaults(level = AccessLevel.PUBLIC)
  public static class ClassWithApiModelOverride implements TestInterface {
    @NotNull String testString;
    String a;
    String b;
    @JsonProperty("jsontypeinfo") String c;
    @ApiModelProperty(name = "apimodelproperty") String d;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PUBLIC)
  @OneOfField(fields = {"x", "y"})
  public static class ClassWithoutApiModelOverride implements TestInterface {
    String testString;
    String x;
    String y;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PUBLIC)
  public static class ClassWhichContainsInterface {
    @NotNull Types type;
    @JsonProperty("spec")
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
    TestInterface testInterface;
  }

  public enum Types { ClassWithApiModelOverride, ClassWithoutApiModelOverride }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PUBLIC)
  public static class ClassWhichContainsInterfaceWithInternal {
    String mnop;
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true)
    TestInterface testInterface;
  }

  @JsonSubTypes({
    @JsonSubTypes.Type(value = InterfaceImpl1.class, name = "InterfaceImpl1")
    , @JsonSubTypes.Type(value = InterfaceImpl2.class, name = "InterfaceImpl2")
  })
  public interface TestInterface1 {}

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PUBLIC)
  public static class InterfaceImpl1 {
    String type;
    String abc;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PUBLIC)
  public static class InterfaceImpl2 {
    String type;
    String xyz;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PUBLIC)
  public static class ClassWhichContainsInterfaceWithInternalWithList {
    String mnop;
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME, property = "mnop", include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true)
    List<TestInterface> testInterface;
  }
}

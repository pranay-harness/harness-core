/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.yaml;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.validation.OneOfField;
import io.harness.validation.OneOfSet;

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
import org.hibernate.validator.constraints.NotEmpty;

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

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @OneOfSet(fields = {"type,testInterface", "b, d", "testString,", "e, f"})
  @FieldDefaults(level = AccessLevel.PUBLIC)
  public static class ClassWithOneOfSetAnnotation {
    @NotNull String testString;
    String a;
    @NotEmpty String b;
    @NotNull @JsonProperty("jsontypeinfo") String c;
    @ApiModelProperty(name = "apimodelproperty") String d;
    @NotNull Types type;
    @JsonProperty("spec")
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
    TestInterface testInterface;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PUBLIC)
  @OneOfSet(fields = {"a,mnop", "testInterface"})
  public static class ClassWhichContainsInterfaceWithInternalAndOneOfSetAnnotation {
    String mnop;
    @NotNull String a;
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true)
    TestInterface testInterface;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PUBLIC)
  @OneOfField(fields = {"x", "y"})
  @OneOfSet(fields = {"x", "y"})
  public static class ClassWithBothOneOfSetAndOneOfFieldAnnotation {
    String testString;
    String x;
    String y;
  }
}

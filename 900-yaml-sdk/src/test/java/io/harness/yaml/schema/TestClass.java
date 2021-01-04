package io.harness.yaml.schema;

import io.harness.EntityType;
import io.harness.validation.OneOfField;
import io.harness.yaml.YamlSchemaRoot;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

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

  @YamlSchemaRoot(EntityType.CONNECTORS)
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
}

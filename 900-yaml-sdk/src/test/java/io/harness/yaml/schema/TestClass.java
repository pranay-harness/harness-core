package io.harness.yaml.schema;

import io.harness.yamlSchema.YamlSchemaRoot;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import javax.validation.constraints.NotNull;

public class TestClass {
  @JsonSubTypes({
    @JsonSubTypes.Type(value = ClassWithApiModelOverride.class, name = "ClassWithApiModelOverride")
    , @JsonSubTypes.Type(value = ClassWithoutApiModelOverride.class, name = "ClassWithoutApiModelOverride")
  })
  public interface TestInterface {}

  @ApiModel(value = "testName")
  public static class ClassWithApiModelOverride implements TestInterface {
    String testString;
  }

  public static class ClassWithoutApiModelOverride implements TestInterface { String testString; }

  @YamlSchemaRoot("testRoot")
  public static class ClassWhichContainsInterface {
    @NotNull Types type;
    @JsonProperty("spec")
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
    TestInterface testInterface;
  }

  public enum Types { ClassWithApiModelOverride, ClassWithoutApiModelOverride }
}

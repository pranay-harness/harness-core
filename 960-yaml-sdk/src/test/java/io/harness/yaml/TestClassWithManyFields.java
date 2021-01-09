package io.harness.yaml;

import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.list;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.map;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.number;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.EntityType;
import io.harness.yaml.schema.YamlSchemaRoot;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

public class TestClassWithManyFields {
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PUBLIC)
  public static class ClassWithoutApiModelOverride extends TestAbstractClass {
    String testString;
    @YamlSchemaTypes(value = {list, map}, defaultType = list) TestRandomClass1 testRandomClass1;
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
    TestAbstractClass abstractClass;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PUBLIC)
  @JsonSubTypes(
      { @JsonSubTypes.Type(value = ClassWithoutApiModelOverride.class, name = "ClassWithoutApiModelOverride") })
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

  public enum Types { ClassWithoutApiModelOverride }
}

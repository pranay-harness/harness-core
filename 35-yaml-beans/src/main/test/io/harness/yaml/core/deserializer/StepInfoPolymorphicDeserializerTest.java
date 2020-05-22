package io.harness.yaml.core.deserializer;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.core.Execution;
import io.harness.yaml.core.intfc.StepInfo;
import lombok.SneakyThrows;
import lombok.Value;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;

public class StepInfoPolymorphicDeserializerTest extends CategoryTest {
  private ObjectMapper mapper;
  private StepPolymorphicDeserializer deserializer;
  private Class<?> type;
  private String typePropertyName;
  private static final String stepYamlFormat = "steps:\n"
      + "   - step:\n"
      + "      identifier: git-clone\n"
      + "      name: git-clone\n"
      + "%s"
      + "      retry: 2\n"
      + "      timeout: 30";

  @Before
  public void setUp() {
    mapper = new ObjectMapper(new YAMLFactory());
    deserializer = new StepPolymorphicDeserializer();
    type = deserializer.getType();
    typePropertyName = deserializer.getTypePropertyName();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testDeserializerDefinition() {
    assertThat(type).isEqualTo(StepInfo.class);
    assertThat(typePropertyName).isEqualTo("type");
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testDeserialization() {
    String stepYaml = String.format(stepYamlFormat, "      type: testType\n");

    Execution deserialize = deserialize(stepYaml);
    assertThat(deserialize.getSteps().get(0)).isInstanceOf(type);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testDeserializationWithUndefinedType() {
    String stepYaml = String.format(stepYamlFormat, "      type: unknown\n");
    Execution deserialize = deserialize(stepYaml);
    assertThat(deserialize.getSteps().get(0)).isNull();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testDeserializationWithoutType() {
    String stepYaml = String.format(stepYamlFormat, "");
    Execution deserialize = deserialize(stepYaml);
    assertThat(deserialize.getSteps().get(0)).isNull();
  }

  @SneakyThrows({JsonParseException.class, IOException.class})
  private Execution deserialize(String stepYaml) {
    SimpleModule module = new SimpleModule();
    module.addDeserializer(StepInfo.class, deserializer);
    module.registerSubtypes(new NamedType(TestStepInfo.class, "testType"));
    mapper.registerModule(module);
    return mapper.readValue(stepYaml, Execution.class);
  }

  @Value
  public static class TestStepInfo implements StepInfo {
    String name;
    int retry;
    int timeout;
    String identifier;
    String type;
  }
}
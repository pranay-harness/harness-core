package io.harness.yaml.core.deserializer;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import lombok.Builder;
import lombok.Data;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.List;

public class PropertyBindingPolymorphicDeserializerTest extends CategoryTest {
  private static final String PROPERTY_NAME_A = "propertyNameA";
  private static final String PROPERTY_NAME_B = "propertyNameB";
  private static final String PROPERTY_NAME_UNKNOWN = "propertyNameUnknown";

  private static final String VALUE_A = "valueA";
  private static final String VALUE_B = "valueB";

  private ObjectMapper mapper;

  private static class TestDeserializer extends PropertyBindingPolymorphicDeserializer<TestInterface> {
    private static final long serialVersionUID = 1L;

    TestDeserializer() {
      super(TestInterface.class);
      registerBinding(PROPERTY_NAME_A, TestClassA.class);
      registerBinding(PROPERTY_NAME_B, TestClassB.class);
    }
  }

  @Before
  public void setUp() {
    mapper = new ObjectMapper(new YAMLFactory());
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldDeserializeTestClassA() throws IOException {
    TestInterface testInterface = mapper.readValue(PROPERTY_NAME_A + ": " + VALUE_A, TestInterface.class);
    assertThat(testInterface).isInstanceOf(TestClassA.class);
    assertThat(((TestClassA) testInterface).getPropertyNameA()).isEqualTo(VALUE_A);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldDeserializeTestClassB() throws IOException {
    TestInterface testInterface = mapper.readValue(PROPERTY_NAME_B + ": " + VALUE_B, TestInterface.class);
    assertThat(testInterface).isInstanceOf(TestClassB.class);
    assertThat(((TestClassB) testInterface).getPropertyNameB()).isEqualTo(VALUE_B);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldDeserializeListOfTestInterfaces() throws IOException {
    String testString = "- " + PROPERTY_NAME_A + ": " + VALUE_A + '\n' + "- " + PROPERTY_NAME_A + ": " + VALUE_A + '\n'
        + "- " + PROPERTY_NAME_B + ": " + VALUE_B;
    List<TestInterface> testInterfaceList = mapper.readValue(testString, new TypeReference<List<TestInterface>>() {});

    assertThat(testInterfaceList).hasSize(3);

    assertThat(testInterfaceList)
        .containsSequence(TestClassA.builder().propertyNameA(VALUE_A).build(),
            TestClassA.builder().propertyNameA(VALUE_A).build(), TestClassB.builder().propertyNameB(VALUE_B).build());
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldFailOnUnknownBinding() {
    String testString = PROPERTY_NAME_UNKNOWN + ": randomValue";
    assertThatThrownBy(() -> mapper.readValue(testString, TestInterface.class))
        .isInstanceOf(JsonMappingException.class)
        .hasMessageContaining("No registered binding found for deserialization");
  }

  @JsonDeserialize(using = TestDeserializer.class)
  private interface TestInterface {}

  @Data
  @Builder
  @JsonDeserialize
  private static class TestClassA implements TestInterface {
    private String propertyNameA;
  }

  @Data
  @Builder
  @JsonDeserialize
  private static class TestClassB implements TestInterface {
    private String propertyNameB;
  }
}

package io.harness.yaml.core.deserializer;

import static io.harness.rule.OwnerRule.ALEKSANDAR;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.serializer.AnnotationAwareJsonSubtypeResolver;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TypeAwarePolymorphicDeserializerTest extends CategoryTest {
  private static final String TYPE_A = "typeA";
  private static final String TYPE_B = "typeB";
  private static final String TYPE_PROPERTY = "type";
  private static final String OTHER_PROPERTY = "other";
  private static final String TYPE_UNKNOWN = "typeUnknown";

  private ObjectMapper mapper;

  private static class TestDeserializer extends TypeAwarePolymorphicDeserializer<TestInterface> {
    private static final long serialVersionUID = 1L;

    @Override
    public Class<?> getType() {
      return TestInterface.class;
    }

    @Override
    public String getTypePropertyName() {
      return "type";
    }
  }

  @Before
  public void setUp() {
    mapper = new ObjectMapper(new YAMLFactory());
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.setSubtypeResolver(AnnotationAwareJsonSubtypeResolver.newInstance(mapper.getSubtypeResolver()));
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldDeserializeTestClassA() throws IOException {
    TestInterface testInterface = mapper.readValue(TYPE_PROPERTY + ": " + TYPE_A, TestInterface.class);

    assertThat(testInterface).isInstanceOf(TestClassA.class);
    assertThat(testInterface.getType()).isEqualTo(TYPE_A);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldDeserializeTestClassB() throws IOException {
    TestInterface testInterface = mapper.readValue(TYPE_PROPERTY + ": " + TYPE_B, TestInterface.class);

    assertThat(testInterface).isInstanceOf(TestClassB.class);
    assertThat(testInterface.getType()).isEqualTo(TYPE_B);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldDeserializeListOfTestInterfaces() throws IOException {
    String testString = "- " + TYPE_PROPERTY + ": " + TYPE_A + '\n' + "- " + TYPE_PROPERTY + ": " + TYPE_A + '\n' + "- "
        + TYPE_PROPERTY + ": " + TYPE_B;

    List<TestInterface> testInterfaceList = mapper.readValue(testString, new TypeReference<List<TestInterface>>() {});

    assertThat(testInterfaceList).hasSize(3);
    assertThat(testInterfaceList)
        .containsSequence(TestClassA.builder().type(TYPE_A).build(), TestClassA.builder().type(TYPE_A).build(),
            TestClassB.builder().type(TYPE_B).build());
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldFailOnUnknownType() {
    String testString = TYPE_PROPERTY + ": " + TYPE_UNKNOWN;
    assertThatThrownBy(() -> mapper.readValue(testString, TestInterface.class))
        .isInstanceOf(JsonMappingException.class)
        .hasMessageContaining("No class definition found for type: 'typeUnknown'");
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldFailOnEmptyType() {
    String testString = TYPE_PROPERTY + ": ";
    assertThatThrownBy(() -> mapper.readValue(testString, TestInterface.class))
        .isInstanceOf(JsonMappingException.class)
        .hasMessageContaining("Type property: '" + TYPE_PROPERTY + "' is empty");
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldFailOnUnknownDefinition() {
    String testString = OTHER_PROPERTY + ": " + TYPE_A;
    assertThatThrownBy(() -> mapper.readValue(testString, TestInterface.class))
        .isInstanceOf(JsonMappingException.class)
        .hasMessageContaining("Cannot find type property: '" + TYPE_PROPERTY + "'");
  }

  @JsonDeserialize(using = TestDeserializer.class)
  private interface TestInterface {
    String getType();
  }

  @Data
  @Builder
  @JsonTypeName(TYPE_A)
  private static class TestClassA implements TestInterface {
    private String type;
  }

  @Data
  @Builder
  @JsonTypeName(TYPE_B)
  private static class TestClassB implements TestInterface {
    private String type;
  }
}

package io.harness.yaml.schema;

import static io.harness.NGCommonEntityConstants.ORG_KEY;
import static io.harness.NGCommonEntityConstants.PROJECT_KEY;
import static io.harness.rule.OwnerRule.ABHINAV;
import static io.harness.yaml.schema.beans.SchemaConstants.CONST_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.ENUM_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.REQUIRED_NODE;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.CategoryTest;
import io.harness.EntityType;
import io.harness.category.element.UnitTests;
import io.harness.encryption.Scope;
import io.harness.rule.Owner;
import io.harness.yaml.TestClass;
import io.harness.yaml.YamlSdkInitConstants;
import io.harness.yaml.utils.YamlSchemaUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({YamlSchemaUtils.class, IOUtils.class})
public class YamlSchemaProviderTest extends CategoryTest {
  YamlSchemaHelper yamlSchemaHelper = new YamlSchemaHelper();
  YamlSchemaProvider yamlSchemaProvider = new YamlSchemaProvider(yamlSchemaHelper);
  String schema;

  @Before
  public void setup() throws IOException {
    initializeSchemaMapAndGetSchema();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGetYamlSchema() throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    final JsonNode yamlSchema = yamlSchemaProvider.getYamlSchema(EntityType.CONNECTORS, null, null, null);
    final JsonNode yamlSchema_1 = yamlSchemaProvider.getYamlSchema(EntityType.CONNECTORS, "abc", null, Scope.ORG);
    final JsonNode yamlSchema_2 = yamlSchemaProvider.getYamlSchema(EntityType.CONNECTORS, "abc", "xyz", Scope.PROJECT);
    final JsonNode yamlSchema_3 = yamlSchemaProvider.getYamlSchema(EntityType.CONNECTORS, null, null, Scope.PROJECT);
    final JsonNode yamlSchema_4 = yamlSchemaProvider.getYamlSchema(EntityType.CONNECTORS, null, null, Scope.ORG);
    final JsonNode yamlSchema_5 = yamlSchemaProvider.getYamlSchema(EntityType.CONNECTORS, null, null, Scope.ACCOUNT);

    assertThat(yamlSchema).isNotNull();
    assertThat(yamlSchema).isEqualTo(objectMapper.readTree(schema));
    assertThat(yamlSchemaProvider.getSecondLevelNodeProperties(yamlSchemaProvider.getSecondLevelNode(yamlSchema_1))
                   .get(ORG_KEY)
                   .get(CONST_NODE)
                   .textValue())
        .isEqualTo("abc");
    assertThat(yamlSchemaProvider.getSecondLevelNodeProperties(yamlSchemaProvider.getSecondLevelNode(yamlSchema_1))
                   .get(PROJECT_KEY))
        .isNull();
    assertRequiredNode(yamlSchema_1, ORG_KEY);
    assertThat(yamlSchemaProvider.getSecondLevelNodeProperties(yamlSchemaProvider.getSecondLevelNode(yamlSchema_2))
                   .get(ORG_KEY)
                   .get(CONST_NODE)
                   .textValue())
        .isEqualTo("abc");
    assertThat(yamlSchemaProvider.getSecondLevelNodeProperties(yamlSchemaProvider.getSecondLevelNode(yamlSchema_2))
                   .get(PROJECT_KEY)
                   .get(CONST_NODE)
                   .textValue())
        .isEqualTo("xyz");
    assertRequiredNode(yamlSchema_2, ORG_KEY);
    assertRequiredNode(yamlSchema_2, PROJECT_KEY);
    assertThat(yamlSchemaProvider.getSecondLevelNodeProperties(yamlSchemaProvider.getSecondLevelNode(yamlSchema_3))
                   .get(ORG_KEY))
        .isNotNull();
    assertThat(yamlSchemaProvider.getSecondLevelNodeProperties(yamlSchemaProvider.getSecondLevelNode(yamlSchema_3))
                   .get(PROJECT_KEY))
        .isNotNull();
    assertRequiredNode(yamlSchema_3, ORG_KEY);
    assertRequiredNode(yamlSchema_3, PROJECT_KEY);
    assertThat(yamlSchemaProvider.getSecondLevelNodeProperties(yamlSchemaProvider.getSecondLevelNode(yamlSchema_4))
                   .get(ORG_KEY))
        .isNotNull();
    assertThat(yamlSchemaProvider.getSecondLevelNodeProperties(yamlSchemaProvider.getSecondLevelNode(yamlSchema_4))
                   .get(PROJECT_KEY))
        .isNull();
    assertRequiredNode(yamlSchema_4, ORG_KEY);
    assertThat(yamlSchemaProvider.getSecondLevelNodeProperties(yamlSchemaProvider.getSecondLevelNode(yamlSchema_5))
                   .get(ORG_KEY))
        .isNull();
    assertThat(yamlSchemaProvider.getSecondLevelNodeProperties(yamlSchemaProvider.getSecondLevelNode(yamlSchema_5))
                   .get(PROJECT_KEY))
        .isNull();
  }

  private void assertRequiredNode(JsonNode yamlschema, String key) {
    AtomicBoolean requiredStatus = new AtomicBoolean(false);
    yamlSchemaProvider.getSecondLevelNode(yamlschema).get(REQUIRED_NODE).iterator().forEachRemaining(requiredNode -> {
      requiredStatus.set(requiredStatus.get() || requiredNode.textValue().equals(key));
    });
    assertThat(requiredStatus.get()).isTrue();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGetYamlSchemaWithFieldSetAtSecondLevel() {
    final JsonNode yamlSchema = yamlSchemaProvider.getYamlSchemaWithArrayFieldUpdatedAtSecondLevel(
        EntityType.CONNECTORS, null, null, null, "type", ENUM_NODE, "XYZ");
    assertThat(yamlSchemaProvider.getSecondLevelNodeProperties(yamlSchemaProvider.getSecondLevelNode(yamlSchema))
                   .get("type")
                   .get(ENUM_NODE)
                   .elements()
                   .next()
                   .textValue())
        .isEqualTo("XYZ");
  }

  private void initializeSchemaMapAndGetSchema() throws IOException {
    yamlSchemaHelper = new YamlSchemaHelper();
    schema = getResource("testSchema/sampleSchema.json");
    mockStatic(YamlSchemaUtils.class);
    mockStatic(IOUtils.class);
    Set<Class<?>> classes = new HashSet<>();
    // Note: the schema and class used for testing are not in sync.
    classes.add(TestClass.ClassWhichContainsInterface.class);
    when(YamlSchemaUtils.getClasses(any())).thenReturn(classes);
    when(IOUtils.resourceToString(any(), any(), any())).thenReturn(schema);
    yamlSchemaHelper.initializeSchemaMaps(YamlSdkInitConstants.schemaBasePath, classes);
  }

  private String getResource(String resource) throws IOException {
    return IOUtils.resourceToString(resource, StandardCharsets.UTF_8, this.getClass().getClassLoader());
  }
}
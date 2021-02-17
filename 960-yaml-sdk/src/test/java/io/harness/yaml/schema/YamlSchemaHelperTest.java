package io.harness.yaml.schema;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.CategoryTest;
import io.harness.EntityType;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.TestClass;
import io.harness.yaml.YamlSdkInitConstants;
import io.harness.yaml.schema.beans.YamlSchemaRootClass;
import io.harness.yaml.schema.beans.YamlSchemaWithDetails;
import io.harness.yaml.utils.YamlSchemaUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({YamlSchemaUtils.class, IOUtils.class})
public class YamlSchemaHelperTest extends CategoryTest {
  YamlSchemaHelper yamlSchemaHelper;

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void initializeSchemaMapAndGetSchema() throws IOException {
    final List<YamlSchemaRootClass> yamlSchemaRootClasses =
        Arrays.asList((YamlSchemaRootClass.builder()
                           .entityType(EntityType.CONNECTORS)
                           .clazz(TestClass.ClassWhichContainsInterface.class)
                           .build()));

    yamlSchemaHelper = new YamlSchemaHelper(yamlSchemaRootClasses);
    String schema = getResource("testSchema/testOutputSchema.json");
    ObjectMapper objectMapper = new ObjectMapper();
    final JsonNode jsonNode = objectMapper.readTree(schema);

    YamlSchemaGenerator yamlSchemaGenerator =
        new YamlSchemaGenerator(new JacksonClassHelper(), new SwaggerGenerator(), yamlSchemaRootClasses);
    Map<EntityType, JsonNode> entityTypeJsonNodeMap = yamlSchemaGenerator.generateYamlSchema();
    yamlSchemaHelper.initializeSchemaMaps(entityTypeJsonNodeMap);
    final YamlSchemaWithDetails schemaForEntityType =
        yamlSchemaHelper.getSchemaDetailsForEntityType(EntityType.CONNECTORS);
    assertThat(schemaForEntityType).isNotNull();
    assertThat(schemaForEntityType.getSchema()).isEqualTo(jsonNode);
  }

  private String getResource(String resource) throws IOException {
    return IOUtils.resourceToString(resource, StandardCharsets.UTF_8, this.getClass().getClassLoader());
  }
}
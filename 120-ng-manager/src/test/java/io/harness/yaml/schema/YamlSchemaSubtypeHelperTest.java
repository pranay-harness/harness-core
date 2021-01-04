package io.harness.yaml.schema;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.EntitySubtype;
import io.harness.EntityType;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.rule.Owner;
import io.harness.yaml.YamlSdkConfiguration;
import io.harness.yaml.utils.YamlConstants;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class YamlSchemaSubtypeHelperTest extends CategoryTest {
  YamlSchemaSubtypeHelper yamlSchemaSubtypeHelper = new YamlSchemaSubtypeHelper();

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGetSchemaForSubtypeFromMap() throws IOException {
    final String connectorSchema = getConnectorSchema();
    verifyIfCorrectSubTypeIsReturnedForConnector(
        EntityType.CONNECTORS, ConnectorType.KUBERNETES_CLUSTER, connectorSchema);
    verifyIfCorrectSubTypeIsReturnedForConnector(
        EntityType.CONNECTORS, ConnectorType.KUBERNETES_CLUSTER, connectorSchema);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGetSchemaForSubtype() throws IOException {
    final String connectorSchema = getConnectorSchema();
    verifyIfCorrectSubTypeIsReturnedForConnector(
        EntityType.CONNECTORS, ConnectorType.KUBERNETES_CLUSTER, connectorSchema);
    verifyIfCorrectSubTypeIsReturnedForConnector(EntityType.CONNECTORS, ConnectorType.GIT, connectorSchema);
  }

  String getConnectorSchema() throws IOException {
    return IOUtils.resourceToString(YamlSdkConfiguration.schemaBasePath + File.separator
            + EntityType.CONNECTORS.getYamlName() + File.separator + YamlConstants.SCHEMA_FILE_NAME,
        StandardCharsets.UTF_8, YamlSchemaSubtypeHelper.class.getClassLoader());
  }

  void verifyIfCorrectSubTypeIsReturnedForConnector(EntityType entityType, EntitySubtype entitySubtype, String schema)
      throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    final String schemaForSubtype = yamlSchemaSubtypeHelper.getSchemaForSubtype(entityType, entitySubtype, schema);
    final ObjectNode node = (ObjectNode) objectMapper.readTree(schemaForSubtype);
    final ArrayNode enumNode = yamlSchemaSubtypeHelper.getSubtypeNode(node);
    final Iterator<JsonNode> elements = enumNode.elements();
    assertThat(elements.hasNext()).isTrue();
    assertThat(elements.next().asText()).isEqualTo(((ConnectorType) entitySubtype).getDisplayName());
    assertThat(elements.hasNext()).isFalse();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void throwExceptionForSubtypesWithoutSupport() {
    assertThatThrownBy(()
                           -> yamlSchemaSubtypeHelper.getSchemaForSubtype(
                               EntityType.PIPELINES, ConnectorType.KUBERNETES_CLUSTER, "schema"));
  }
}
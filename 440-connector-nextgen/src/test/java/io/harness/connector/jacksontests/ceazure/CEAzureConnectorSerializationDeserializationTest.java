package io.harness.connector.jacksontests.ceazure;

import static io.harness.connector.jacksontests.ConnectorJacksonTestHelper.readFileAsString;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.utils.AzureConnectorTestHelper;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.remote.NGObjectMapperHelper;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CEAzureConnectorSerializationDeserializationTest extends CategoryTest {
  private ObjectMapper objectMapper;

  @Before
  public void setup() {
    objectMapper = new ObjectMapper();
    NGObjectMapperHelper.configureNGObjectMapper(objectMapper);
  }

  @Test
  @Owner(developers = OwnerRule.UTSAV)
  @Category(UnitTests.class)
  public void testSerializationOfConnectorDTO() throws IOException {
    final String sampleConnectorDTOAsJson =
        objectMapper.writeValueAsString(AzureConnectorTestHelper.createConnectorDTO());

    assertThat(sampleConnectorDTOAsJson).isNotBlank();
    assertThat(objectMapper.readTree(sampleConnectorDTOAsJson)).isEqualTo(objectMapper.readTree(jsonResponse()));
  }

  @Test
  @Owner(developers = OwnerRule.UTSAV)
  @Category(UnitTests.class)
  public void testDeserializationOfJsonResponse() throws IOException {
    final ConnectorDTO sampleConnector = objectMapper.readValue(jsonResponse(), ConnectorDTO.class);

    assertThat(sampleConnector.getConnectorInfo().getConnectorType()).isEqualTo(ConnectorType.CE_AZURE);
    assertThat(sampleConnector).isEqualTo(AzureConnectorTestHelper.createConnectorDTO());
  }

  private static String jsonResponse() {
    return readFileAsString("440-connector-nextgen/src/test/resources/ceazure/ceAzureConfigAsJson.json");
  }
}

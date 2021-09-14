/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.gcpccm;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.connector.jacksontests.ConnectorJacksonTestHelper.readFileAsString;
import static io.harness.rule.OwnerRule.UTSAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.utils.GcpConnectorTestHelper;
import io.harness.remote.NGObjectMapperHelper;
import io.harness.rule.Owner;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
@OwnedBy(CE)
public class GcpCcmConnectorSerializationDeserializationTest extends CategoryTest {
  private ObjectMapper objectMapper;

  @Before
  public void setup() {
    objectMapper = new ObjectMapper();
    NGObjectMapperHelper.configureNGObjectMapper(objectMapper);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testSerializationOfConnectorDTO() throws IOException {
    final String sampleConnectorDTOAsJson =
        objectMapper.writeValueAsString(GcpConnectorTestHelper.createConnectorDTO());

    assertThat(objectMapper.readTree(sampleConnectorDTOAsJson)).isEqualTo(objectMapper.readTree(jsonResponse()));
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testDeserializationOfJsonResponse() throws IOException {
    final ConnectorDTO sampleConnector = objectMapper.readValue(jsonResponse(), ConnectorDTO.class);

    assertThat(sampleConnector).isEqualTo(GcpConnectorTestHelper.createConnectorDTO());
  }

  private static String jsonResponse() {
    return readFileAsString("440-connector-nextgen/src/test/resources/gcpccmconnector/gcpCcmConfig.json");
  }
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.jacksontests.ceawsconnector;

import static io.harness.connector.jacksontests.ConnectorJacksonTestHelper.readFileAsString;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.utils.AWSConnectorTestHelper;
import io.harness.remote.NGObjectMapperHelper;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class CEAwsConnectorSerializationDeserializationTest extends CategoryTest {
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
        objectMapper.writeValueAsString(AWSConnectorTestHelper.createConnectorDTOOfCEAws());

    assertThat(objectMapper.readTree(sampleConnectorDTOAsJson)).isEqualTo(objectMapper.readTree(jsonResponse()));
  }

  @Test
  @Owner(developers = OwnerRule.UTSAV)
  @Category(UnitTests.class)
  public void testDeserializationOfJsonResponse() throws IOException {
    final ConnectorDTO sampleConnector = objectMapper.readValue(jsonResponse(), ConnectorDTO.class);

    assertThat(sampleConnector).isEqualTo(AWSConnectorTestHelper.createConnectorDTOOfCEAws());
  }

  private static String jsonResponse() {
    return readFileAsString("440-connector-nextgen/src/test/resources/ceawsconnector/ceAwsConfigAsJson.json");
  }
}

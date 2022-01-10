/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.jacksontests.cek8s;

import static io.harness.connector.jacksontests.ConnectorJacksonTestHelper.readFileAsString;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.utils.CommonTestHelper;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.cek8s.CEKubernetesClusterConfigDTO;
import io.harness.remote.NGObjectMapperHelper;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CEKubernetesClusterConfigSerializationDeserializationTest extends CategoryTest {
  private static ObjectMapper objectMapper;
  private static final CEKubernetesClusterConfigDTO CE_KUBERNETES_CLUSTER_CONFIG_DTO =
      CEKubernetesClusterConfigDTO.builder()
          .connectorRef("account.k8s_cd")
          .featuresEnabled(Arrays.asList(CEFeatures.OPTIMIZATION, CEFeatures.VISIBILITY))
          .build();

  @Before
  public void setup() {
    objectMapper = new ObjectMapper();
    NGObjectMapperHelper.configureNGObjectMapper(objectMapper);
  }

  @Test
  @Owner(developers = OwnerRule.UTSAV)
  @Category(UnitTests.class)
  public void testSerializationOfConnectorDTO() throws IOException {
    final String sampleConnectorDTOAsJson = objectMapper.writeValueAsString(createConnectorDTO());

    assertThat(objectMapper.readTree(sampleConnectorDTOAsJson)).isEqualTo(objectMapper.readTree(jsonResponse()));
  }

  @Test
  @Owner(developers = OwnerRule.UTSAV)
  @Category(UnitTests.class)
  public void testDeserializationOfJsonResponse() throws IOException {
    final ConnectorDTO sampleConnector = objectMapper.readValue(jsonResponse(), ConnectorDTO.class);

    assertThat(sampleConnector.getConnectorInfo().getConnectorType()).isEqualTo(ConnectorType.CE_KUBERNETES_CLUSTER);
    assertThat(sampleConnector).isEqualTo(createConnectorDTO());
  }

  private static String jsonResponse() {
    return readFileAsString("440-connector-nextgen/src/test/resources/cek8s/ceK8sConnectorConfig.json");
  }

  private static ConnectorDTO createConnectorDTO() {
    return CommonTestHelper.createConnectorDTO(ConnectorType.CE_KUBERNETES_CLUSTER, CE_KUBERNETES_CLUSTER_CONFIG_DTO);
  }
}

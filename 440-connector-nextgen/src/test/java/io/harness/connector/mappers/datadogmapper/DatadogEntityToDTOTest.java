/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.mappers.datadogmapper;

import static io.harness.annotations.dev.HarnessTeam.CV;
import static io.harness.rule.OwnerRule.KANHAIYA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.datadogconnector.DatadogConnector;
import io.harness.delegate.beans.connector.datadog.DatadogConnectorDTO;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

@OwnedBy(CV)
public class DatadogEntityToDTOTest extends CategoryTest {
  @InjectMocks private DatadogEntityToDTO datadogEntityToDTO;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testConvertEntitiyToDto() {
    String apiKeyRef = "apiKeyRef";
    String applicationKeyRef = "appKeyRef";
    String url = "https://datadoghq.com";

    DatadogConnector connector =
        DatadogConnector.builder().url(url).apiKeyRef(apiKeyRef).applicationKeyRef(applicationKeyRef).build();

    DatadogConnectorDTO connectorDTO = datadogEntityToDTO.createConnectorDTO(connector);
    assertThat(connectorDTO).isNotNull();
    assertThat(connector.getUrl()).isEqualTo(connectorDTO.getUrl());
    assertThat(connectorDTO.getApiKeyRef().getIdentifier()).isEqualTo(connector.getApiKeyRef());
    assertThat(connectorDTO.getApplicationKeyRef().getIdentifier()).isEqualTo(connector.getApplicationKeyRef());
  }
}

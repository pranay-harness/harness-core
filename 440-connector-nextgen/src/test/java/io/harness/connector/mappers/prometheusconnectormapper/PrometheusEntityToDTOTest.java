/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.prometheusconnectormapper;

import static io.harness.rule.OwnerRule.ANJAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.prometheusconnector.PrometheusConnector;
import io.harness.connector.mappers.prometheusmapper.PrometheusEntityToDTO;
import io.harness.delegate.beans.connector.prometheusconnector.PrometheusConnectorDTO;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.CV)
public class PrometheusEntityToDTOTest extends CategoryTest {
  @InjectMocks PrometheusEntityToDTO prometheusEntityToDTO;

  String urlWithoutSlash = "https://prometheusconnector.com";
  String urlWithSlash = urlWithoutSlash + "/";

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ANJAN)
  @Category(UnitTests.class)
  public void testEntityToPrometheusDTO() {
    PrometheusConnector prometheusConnector = PrometheusConnector.builder().url(urlWithoutSlash).build();
    PrometheusConnectorDTO connectorDTO = prometheusEntityToDTO.createConnectorDTO(prometheusConnector);
    assertThat(connectorDTO).isNotNull();
    assertThat(connectorDTO.getUrl()).isEqualTo(urlWithSlash);

    prometheusConnector = PrometheusConnector.builder().url(urlWithSlash).build();
    connectorDTO = prometheusEntityToDTO.createConnectorDTO(prometheusConnector);
    assertThat(connectorDTO).isNotNull();
    assertThat(connectorDTO.getUrl()).isEqualTo(urlWithSlash);
  }
}

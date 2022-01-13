/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.mappers.prometheusconnectormapper;

import static io.harness.rule.OwnerRule.ANJAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.prometheusconnector.PrometheusConnector;
import io.harness.connector.mappers.prometheusmapper.PrometheusDTOToEntity;
import io.harness.delegate.beans.connector.prometheusconnector.PrometheusConnectorDTO;
import io.harness.rule.Owner;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class PrometheusDTOToEntityTest extends CategoryTest {
  String urlWithoutSlash = "https://promethus.url.com";
  String urlWithSlash = urlWithoutSlash + "/";

  @InjectMocks PrometheusDTOToEntity prometheusDTOToEntity;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ANJAN)
  @Category(UnitTests.class)
  public void testToPrometheusConnector() {
    PrometheusConnectorDTO connectorDTO =
        PrometheusConnectorDTO.builder().url(urlWithoutSlash).delegateSelectors(Collections.emptySet()).build();
    PrometheusConnector prometheusConnector = prometheusDTOToEntity.toConnectorEntity(connectorDTO);
    assertThat(prometheusConnector).isNotNull();
    assertThat(prometheusConnector.getUrl()).isEqualTo(urlWithSlash);

    connectorDTO = PrometheusConnectorDTO.builder().url(urlWithSlash).delegateSelectors(Collections.emptySet()).build();
    prometheusConnector = prometheusDTOToEntity.toConnectorEntity(connectorDTO);
    assertThat(prometheusConnector).isNotNull();
    assertThat(prometheusConnector.getUrl()).isEqualTo(urlWithSlash);
  }
}

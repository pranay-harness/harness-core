/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.dynatraceconnectormapper;

import static io.harness.rule.OwnerRule.ANJAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.dynatraceconnector.DynatraceConnector;
import io.harness.connector.mappers.dynatracemapper.DynatraceEntityToDTO;
import io.harness.delegate.beans.connector.dynatrace.DynatraceConnectorDTO;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class DynatraceEntityToDTOTest extends CategoryTest {
  static final String apiToken = "dynatrace_api_token";
  static final String urlWithoutSlash = "http://dyna.com";
  static final String urlWithSlash = urlWithoutSlash + "/";

  @InjectMocks DynatraceEntityToDTO dynatraceEntityToDTO;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ANJAN)
  @Category(UnitTests.class)
  public void testToPrometheusDTO() {
    DynatraceConnector dynatraceConnector =
        DynatraceConnector.builder().apiTokenRef(apiToken).url(urlWithoutSlash).build();
    DynatraceConnectorDTO dynatraceConnectorDTO = dynatraceEntityToDTO.createConnectorDTO(dynatraceConnector);

    assertThat(dynatraceConnectorDTO.getApiTokenRef().toSecretRefStringValue()).isEqualTo(apiToken);
    assertThat(dynatraceConnectorDTO.getUrl()).isEqualTo(urlWithSlash);

    dynatraceConnector = DynatraceConnector.builder().apiTokenRef(apiToken).url(urlWithSlash).build();
    dynatraceConnectorDTO = dynatraceEntityToDTO.createConnectorDTO(dynatraceConnector);

    assertThat(dynatraceConnectorDTO.getApiTokenRef().toSecretRefStringValue()).isEqualTo(apiToken);
    assertThat(dynatraceConnectorDTO.getUrl()).isEqualTo(urlWithSlash);
  }
}

/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.mappers.dynatraceconnectormapper;

import static io.harness.rule.OwnerRule.ANJAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.dynatraceconnector.DynatraceConnector;
import io.harness.connector.mappers.dynatracemapper.DynatraceDTOToEntity;
import io.harness.delegate.beans.connector.dynatrace.DynatraceConnectorDTO;
import io.harness.encryption.SecretRefHelper;
import io.harness.rule.Owner;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class DynatraceDTOToEntityTest extends CategoryTest {
  static final String urlWithoutSlash = "http://dyna.com";
  static final String urlWithSlash = urlWithoutSlash + "/";
  static final String apiToken = "dynatrace_api_token";

  @InjectMocks DynatraceDTOToEntity dynatraceDTOToEntity;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ANJAN)
  @Category(UnitTests.class)
  public void testToPrometheusConnector() {
    DynatraceConnectorDTO connectorDTO = DynatraceConnectorDTO.builder()
                                             .url(urlWithoutSlash)
                                             .delegateSelectors(Collections.emptySet())
                                             .apiTokenRef(SecretRefHelper.createSecretRef(apiToken))
                                             .build();

    DynatraceConnector dynatraceConnector = dynatraceDTOToEntity.toConnectorEntity(connectorDTO);
    assertThat(dynatraceConnector).isNotNull();
    assertThat(dynatraceConnector.getUrl()).isEqualTo(urlWithSlash);
    assertThat(dynatraceConnector.getApiTokenRef()).isEqualTo(apiToken);

    connectorDTO = DynatraceConnectorDTO.builder()
                       .url(urlWithSlash)
                       .delegateSelectors(Collections.emptySet())
                       .apiTokenRef(SecretRefHelper.createSecretRef(apiToken))
                       .build();

    dynatraceConnector = dynatraceDTOToEntity.toConnectorEntity(connectorDTO);
    assertThat(dynatraceConnector).isNotNull();
    assertThat(dynatraceConnector.getUrl()).isEqualTo(urlWithSlash);
    assertThat(dynatraceConnector.getApiTokenRef()).isEqualTo(apiToken);
  }
}

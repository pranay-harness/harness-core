/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.datadogmapper;

import static io.harness.annotations.dev.HarnessTeam.CV;
import static io.harness.encryption.Scope.ACCOUNT;
import static io.harness.rule.OwnerRule.KANHAIYA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.datadogconnector.DatadogConnector;
import io.harness.delegate.beans.connector.datadog.DatadogConnectorDTO;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

@OwnedBy(CV)
public class DatadogDTOToEntityTest extends CategoryTest {
  @InjectMocks private DatadogDTOToEntity datadogDTOToEntity;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testConvertEntitiyToDto() {
    String url = "https://insights-api.eu.datadog.com";
    SecretRefData apiKeySecretRefData = SecretRefData.builder().identifier("apiKey").scope(ACCOUNT).build();
    SecretRefData applicationKeySecretRefData = SecretRefData.builder().identifier("appKey").scope(ACCOUNT).build();
    DatadogConnectorDTO connectorDTO = DatadogConnectorDTO.builder()
                                           .apiKeyRef(apiKeySecretRefData)
                                           .applicationKeyRef(applicationKeySecretRefData)
                                           .url(url)
                                           .build();

    DatadogConnector connector = datadogDTOToEntity.toConnectorEntity(connectorDTO);
    assertThat(connector).isNotNull();
    assertThat(connector.getUrl()).isEqualTo(connectorDTO.getUrl());
    assertThat(connector.getApiKeyRef())
        .isEqualTo(ACCOUNT.getYamlRepresentation() + "." + connectorDTO.getApiKeyRef().getIdentifier());
    assertThat(connector.getApplicationKeyRef())
        .isEqualTo(ACCOUNT.getYamlRepresentation() + "." + connectorDTO.getApplicationKeyRef().getIdentifier());
  }
}

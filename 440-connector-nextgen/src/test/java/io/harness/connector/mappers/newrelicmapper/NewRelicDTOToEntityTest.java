/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.newrelicmapper;

import static io.harness.encryption.Scope.ACCOUNT;
import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.newrelicconnector.NewRelicConnector;
import io.harness.connector.mappers.newerlicmapper.NewRelicDTOToEntity;
import io.harness.delegate.beans.connector.newrelic.NewRelicConnectorDTO;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class NewRelicDTOToEntityTest extends CategoryTest {
  @InjectMocks private NewRelicDTOToEntity newRelicDTOToEntity;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testConvertEntitiyToDto() {
    String secretIdentifier = "secretIdentifier";
    String accountId = "accountId";
    String url = "https://insights-api.eu.newrelic.com";
    SecretRefData secretRefData = SecretRefData.builder().identifier(secretIdentifier).scope(ACCOUNT).build();

    NewRelicConnectorDTO connectorDTO =
        NewRelicConnectorDTO.builder().newRelicAccountId(accountId).apiKeyRef(secretRefData).build();

    NewRelicConnector connector = newRelicDTOToEntity.toConnectorEntity(connectorDTO);
    assertThat(connector).isNotNull();
    assertThat(connector.getNewRelicAccountId()).isEqualTo(connectorDTO.getNewRelicAccountId());
    assertThat(connector.getUrl()).isEqualTo(connectorDTO.getUrl());
    assertThat(connector.getApiKeyRef())
        .isEqualTo(ACCOUNT.getYamlRepresentation() + "." + connectorDTO.getApiKeyRef().getIdentifier());
  }
}

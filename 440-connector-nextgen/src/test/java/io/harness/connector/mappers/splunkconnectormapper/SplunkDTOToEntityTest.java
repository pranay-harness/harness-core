/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.mappers.splunkconnectormapper;

import static io.harness.encryption.Scope.ACCOUNT;
import static io.harness.encryption.SecretRefData.SECRET_DOT_DELIMINITER;
import static io.harness.rule.OwnerRule.NEMANJA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.splunkconnector.SplunkConnector;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class SplunkDTOToEntityTest extends CategoryTest {
  @InjectMocks SplunkDTOToEntity splunkDTOToEntity;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testToSplunkConnector() {
    String username = "username";
    String encryptedPassword = "encryptedPassword";
    String splunkUrl = "splunkUrl";
    String accountId = "accountId";

    SecretRefData secretRefData = SecretRefData.builder().identifier(encryptedPassword).scope(ACCOUNT).build();
    SplunkConnectorDTO splunkConnectorDTO = SplunkConnectorDTO.builder()
                                                .username(username)
                                                .passwordRef(secretRefData)
                                                .splunkUrl(splunkUrl)
                                                .accountId(accountId)
                                                .build();

    SplunkConnector splunkConnector = splunkDTOToEntity.toConnectorEntity(splunkConnectorDTO);
    assertThat(splunkConnector).isNotNull();
    assertThat(splunkConnector.getUsername()).isEqualTo(splunkConnectorDTO.getUsername());
    assertThat(splunkConnector.getPasswordRef()).isNotNull();
    assertThat(splunkConnector.getPasswordRef())
        .isEqualTo(ACCOUNT.getYamlRepresentation() + SECRET_DOT_DELIMINITER
            + splunkConnectorDTO.getPasswordRef().getIdentifier());
    assertThat(splunkConnector.getSplunkUrl()).isEqualTo(splunkConnectorDTO.getSplunkUrl());
    assertThat(splunkConnector.getAccountId()).isEqualTo(splunkConnectorDTO.getAccountId());
  }
}

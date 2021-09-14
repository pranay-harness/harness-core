/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.helm;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.helm.HttpHelmConnector;
import io.harness.connector.entities.embedded.helm.HttpHelmUsernamePasswordAuthentication;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.helm.HttpHelmAuthType;
import io.harness.delegate.beans.connector.helm.HttpHelmAuthenticationDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmConnectorDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmUsernamePasswordDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class HttpHelmDTOToEntityTest extends CategoryTest {
  private HttpHelmDTOToEntity mapper;

  @Before
  public void setUp() {
    mapper = new HttpHelmDTOToEntity();
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void testAnonymousDtoToConnectorEntity() {
    HttpHelmConnectorDTO dto =
        HttpHelmConnectorDTO.builder()
            .helmRepoUrl("localhost")
            .auth(HttpHelmAuthenticationDTO.builder().authType(HttpHelmAuthType.ANONYMOUS).build())
            .build();

    HttpHelmConnector entity = mapper.toConnectorEntity(dto);
    assertThat(entity).isNotNull();
    assertThat(entity.getType()).isEqualTo(ConnectorType.HTTP_HELM_REPO);
    assertThat(entity.getUrl()).isEqualTo("localhost");
    assertThat(entity.getAuthType()).isEqualTo(HttpHelmAuthType.ANONYMOUS);
    assertThat(entity.getHttpHelmAuthentication()).isNull();
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void testUsernameAndPasswordSecretRefCredentialsDtoToConnectorEntity() {
    SecretRefData usernameSecretRef =
        SecretRefData.builder().identifier("username-secret").scope(Scope.ACCOUNT).build();
    SecretRefData passwordSecretRef =
        SecretRefData.builder().identifier("password-secret").scope(Scope.ACCOUNT).build();

    HttpHelmConnectorDTO dto = HttpHelmConnectorDTO.builder()
                                   .helmRepoUrl("localhost")
                                   .auth(HttpHelmAuthenticationDTO.builder()
                                             .authType(HttpHelmAuthType.USER_PASSWORD)
                                             .credentials(HttpHelmUsernamePasswordDTO.builder()
                                                              .usernameRef(usernameSecretRef)
                                                              .passwordRef(passwordSecretRef)
                                                              .build())
                                             .build())
                                   .build();

    HttpHelmConnector entity = mapper.toConnectorEntity(dto);
    assertThat(entity).isNotNull();
    assertThat(entity.getUrl()).isEqualTo("localhost");
    assertThat(entity.getType()).isEqualTo(ConnectorType.HTTP_HELM_REPO);
    assertThat(entity.getAuthType()).isEqualTo(HttpHelmAuthType.USER_PASSWORD);
    assertThat(entity.getHttpHelmAuthentication()).isNotNull();
    HttpHelmUsernamePasswordAuthentication authentication =
        (HttpHelmUsernamePasswordAuthentication) entity.getHttpHelmAuthentication();
    assertThat(authentication.getPasswordRef()).isEqualTo(passwordSecretRef.toSecretRefStringValue());
    assertThat(authentication.getUsernameRef()).isEqualTo(usernameSecretRef.toSecretRefStringValue());
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void testUsernameAndPasswordOnlySecretRefCredentialsDtoToConnectorEntity() {
    SecretRefData passwordSecretRef =
        SecretRefData.builder().identifier("password-secret").scope(Scope.ACCOUNT).build();

    HttpHelmConnectorDTO dto =
        HttpHelmConnectorDTO.builder()
            .helmRepoUrl("localhost")
            .auth(HttpHelmAuthenticationDTO.builder()
                      .authType(HttpHelmAuthType.USER_PASSWORD)
                      .credentials(
                          HttpHelmUsernamePasswordDTO.builder().username("test").passwordRef(passwordSecretRef).build())
                      .build())
            .build();

    HttpHelmConnector entity = mapper.toConnectorEntity(dto);
    assertThat(entity).isNotNull();
    assertThat(entity.getUrl()).isEqualTo("localhost");
    assertThat(entity.getType()).isEqualTo(ConnectorType.HTTP_HELM_REPO);
    assertThat(entity.getAuthType()).isEqualTo(HttpHelmAuthType.USER_PASSWORD);
    assertThat(entity.getHttpHelmAuthentication()).isNotNull();
    HttpHelmUsernamePasswordAuthentication authentication =
        (HttpHelmUsernamePasswordAuthentication) entity.getHttpHelmAuthentication();
    assertThat(authentication.getPasswordRef()).isEqualTo(passwordSecretRef.toSecretRefStringValue());
    assertThat(authentication.getUsernameRef()).isNull();
    assertThat(authentication.getUsername()).isEqualTo("test");
  }
}

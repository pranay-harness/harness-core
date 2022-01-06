/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.mappers.helm;

import static io.harness.encryption.Scope.ACCOUNT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.helm.HttpHelmConnector;
import io.harness.connector.entities.embedded.helm.HttpHelmUsernamePasswordAuthentication;
import io.harness.delegate.beans.connector.helm.HttpHelmAuthType;
import io.harness.delegate.beans.connector.helm.HttpHelmConnectorDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmUsernamePasswordDTO;
import io.harness.encryption.SecretRefHelper;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class HttpHelmEntityToDTOTest extends CategoryTest {
  private HttpHelmEntityToDTO mapper;

  @Before
  public void setUp() {
    mapper = new HttpHelmEntityToDTO();
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void testAnonymousAuthEntityToDto() {
    HttpHelmConnector entity =
        HttpHelmConnector.builder().url("localhost").authType(HttpHelmAuthType.ANONYMOUS).build();

    HttpHelmConnectorDTO dto = mapper.createConnectorDTO(entity);
    assertThat(dto).isNotNull();
    assertThat(dto.getHelmRepoUrl()).isEqualTo("localhost");
    assertThat(dto.getAuth().getAuthType()).isEqualTo(HttpHelmAuthType.ANONYMOUS);
    assertThat(dto.getAuth().getCredentials()).isNull();
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void testUsernameAndPasswordAuthEntityToDto() {
    String usernameSecretRef = ACCOUNT.getYamlRepresentation() + ".usernameRef";
    String passwordSecretRef = ACCOUNT.getYamlRepresentation() + ".passwordRef";
    HttpHelmConnector entity = HttpHelmConnector.builder()
                                   .url("localhost")
                                   .authType(HttpHelmAuthType.USER_PASSWORD)
                                   .httpHelmAuthentication(HttpHelmUsernamePasswordAuthentication.builder()
                                                               .usernameRef(usernameSecretRef)
                                                               .passwordRef(passwordSecretRef)
                                                               .build())
                                   .build();

    HttpHelmConnectorDTO dto = mapper.createConnectorDTO(entity);
    assertThat(dto).isNotNull();
    assertThat(dto.getHelmRepoUrl()).isEqualTo("localhost");
    assertThat(dto.getAuth().getAuthType()).isEqualTo(HttpHelmAuthType.USER_PASSWORD);
    assertThat(dto.getAuth().getCredentials()).isNotNull();
    HttpHelmUsernamePasswordDTO usernamePasswordDTO = (HttpHelmUsernamePasswordDTO) dto.getAuth().getCredentials();
    assertThat(usernamePasswordDTO.getUsername()).isNull();
    assertThat(usernamePasswordDTO.getUsernameRef()).isEqualTo(SecretRefHelper.createSecretRef(usernameSecretRef));
    assertThat(usernamePasswordDTO.getPasswordRef()).isEqualTo(SecretRefHelper.createSecretRef(passwordSecretRef));
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void testUsernameAndPasswordOnlySecretAuthEntityToDto() {
    String passwordSecretRef = ACCOUNT.getYamlRepresentation() + ".passwordRef";
    HttpHelmConnector entity = HttpHelmConnector.builder()
                                   .url("localhost")
                                   .authType(HttpHelmAuthType.USER_PASSWORD)
                                   .httpHelmAuthentication(HttpHelmUsernamePasswordAuthentication.builder()
                                                               .username("test")
                                                               .passwordRef(passwordSecretRef)
                                                               .build())
                                   .build();

    HttpHelmConnectorDTO dto = mapper.createConnectorDTO(entity);
    assertThat(dto).isNotNull();
    assertThat(dto.getHelmRepoUrl()).isEqualTo("localhost");
    assertThat(dto.getAuth().getAuthType()).isEqualTo(HttpHelmAuthType.USER_PASSWORD);
    assertThat(dto.getAuth().getCredentials()).isNotNull();
    HttpHelmUsernamePasswordDTO usernamePasswordDTO = (HttpHelmUsernamePasswordDTO) dto.getAuth().getCredentials();
    assertThat(usernamePasswordDTO.getUsername()).isEqualTo("test");
    assertThat(usernamePasswordDTO.getUsernameRef().getIdentifier()).isNull();
    assertThat(usernamePasswordDTO.getUsernameRef().getScope()).isNull();
    assertThat(usernamePasswordDTO.getPasswordRef()).isEqualTo(SecretRefHelper.createSecretRef(passwordSecretRef));
  }
}

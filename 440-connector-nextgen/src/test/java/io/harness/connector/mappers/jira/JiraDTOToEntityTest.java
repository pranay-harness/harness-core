/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.jira;

import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.jira.JiraConnector;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretRefHelper;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class JiraDTOToEntityTest extends CategoryTest {
  @InjectMocks JiraDTOToEntity jiraDTOToEntity;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testToConnectorEntity() {
    String jiraUrl = "url";
    String userName = "userName";
    String passwordRefIdentifier = "passwordRefIdentifier";
    SecretRefData passwordSecretRef =
        SecretRefData.builder().identifier(passwordRefIdentifier).scope(Scope.ACCOUNT).build();

    JiraConnectorDTO dto =
        JiraConnectorDTO.builder().jiraUrl(jiraUrl).passwordRef(passwordSecretRef).username(userName).build();
    JiraConnector jiraConnector = jiraDTOToEntity.toConnectorEntity(dto);
    assertThat(jiraConnector).isNotNull();
    assertThat(jiraConnector.getJiraUrl()).isEqualTo(jiraUrl);
    assertThat(jiraConnector.getUsername()).isEqualTo(userName);
    assertThat(jiraConnector.getUsernameRef()).isNull();
    assertThat(jiraConnector.getPasswordRef()).isEqualTo(SecretRefHelper.getSecretConfigString(passwordSecretRef));
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testToConnectorEntityUsernameRef() {
    String jiraUrl = "url";
    String userNameRefIdentifier = "userNameRef";
    SecretRefData usernameSecretRef =
        SecretRefData.builder().identifier(userNameRefIdentifier).scope(Scope.ACCOUNT).build();
    String passwordRefIdentifier = "passwordRefIdentifier";
    SecretRefData passwordSecretRef =
        SecretRefData.builder().identifier(passwordRefIdentifier).scope(Scope.ACCOUNT).build();

    JiraConnectorDTO dto = JiraConnectorDTO.builder()
                               .jiraUrl(jiraUrl)
                               .passwordRef(passwordSecretRef)
                               .usernameRef(usernameSecretRef)
                               .build();
    JiraConnector jiraConnector = jiraDTOToEntity.toConnectorEntity(dto);
    assertThat(jiraConnector).isNotNull();
    assertThat(jiraConnector.getJiraUrl()).isEqualTo(jiraUrl);
    assertThat(jiraConnector.getUsername()).isNull();
    assertThat(jiraConnector.getUsernameRef()).isEqualTo(SecretRefHelper.getSecretConfigString(usernameSecretRef));
    assertThat(jiraConnector.getPasswordRef()).isEqualTo(SecretRefHelper.getSecretConfigString(passwordSecretRef));
  }
}

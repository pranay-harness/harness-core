/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.mappers.jira;

import static io.harness.encryption.Scope.ACCOUNT;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.jira.JiraConnector;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.encryption.SecretRefHelper;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class JiraEntityToDTOTest extends CategoryTest {
  @InjectMocks JiraEntityToDTO jiraEntityToDTO;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testCreateConnectorDTO() {
    String jiraUrl = "url";
    String userName = "userName";
    String passwordRef = ACCOUNT + ".passwordRef";

    JiraConnector jiraConnector =
        JiraConnector.builder().jiraUrl(jiraUrl).username(userName).passwordRef(passwordRef).build();

    JiraConnectorDTO jiraConnectorDTO = jiraEntityToDTO.createConnectorDTO(jiraConnector);
    assertThat(jiraConnectorDTO).isNotNull();
    assertThat(jiraConnectorDTO.getJiraUrl()).isEqualTo(jiraUrl);
    assertThat(jiraConnectorDTO.getUsername()).isEqualTo(userName);
    assertThat(jiraConnectorDTO.getUsernameRef().isNull()).isTrue();
    assertThat(jiraConnectorDTO.getPasswordRef()).isEqualTo(SecretRefHelper.createSecretRef(passwordRef));
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testCreateConnectorDTOUsernameRef() {
    String jiraUrl = "url";
    String userNameRef = ACCOUNT + ".userName";
    String passwordRef = ACCOUNT + ".passwordRef";

    JiraConnector jiraConnector =
        JiraConnector.builder().jiraUrl(jiraUrl).usernameRef(userNameRef).passwordRef(passwordRef).build();

    JiraConnectorDTO jiraConnectorDTO = jiraEntityToDTO.createConnectorDTO(jiraConnector);
    assertThat(jiraConnectorDTO).isNotNull();
    assertThat(jiraConnectorDTO.getJiraUrl()).isEqualTo(jiraUrl);
    assertThat(jiraConnectorDTO.getUsername()).isNull();
    assertThat(jiraConnectorDTO.getUsernameRef()).isEqualTo(SecretRefHelper.createSecretRef(userNameRef));
    assertThat(jiraConnectorDTO.getPasswordRef()).isEqualTo(SecretRefHelper.createSecretRef(passwordRef));
  }
}

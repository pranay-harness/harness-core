/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.delegate.task.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.delegate.task.jira.JiraTaskNGParameters.JiraTaskNGParametersBuilder;
import io.harness.encryption.SecretRefData;
import io.harness.jira.JiraClient;
import io.harness.rule.Owner;

import java.util.Collections;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@OwnedBy(CDC)
@RunWith(PowerMockRunner.class)
@PrepareForTest({JiraClient.class, JiraTaskNGHandler.class})
@PowerMockIgnore({"javax.net.ssl.*"})
public class JiraTaskNGHandlerTest extends CategoryTest {
  private final JiraTaskNGHandler jiraTaskNGHandler = new JiraTaskNGHandler();

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testValidateCredentials() throws Exception {
    JiraClient jiraClient = Mockito.mock(JiraClient.class);
    when(jiraClient.getProjects()).thenReturn(Collections.emptyList());
    PowerMockito.whenNew(JiraClient.class).withAnyArguments().thenReturn(jiraClient);

    assertThatCode(() -> jiraTaskNGHandler.validateCredentials(createJiraTaskParametersBuilder().build()))
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testValidateCredentialsError() throws Exception {
    JiraClient jiraClient = Mockito.mock(JiraClient.class);
    when(jiraClient.getProjects()).thenThrow(new RuntimeException("exception"));
    PowerMockito.whenNew(JiraClient.class).withAnyArguments().thenReturn(jiraClient);

    assertThatThrownBy(() -> jiraTaskNGHandler.validateCredentials(createJiraTaskParametersBuilder().build()))
        .isNotNull();
  }

  private JiraTaskNGParametersBuilder createJiraTaskParametersBuilder() {
    JiraConnectorDTO jiraConnectorDTO =
        JiraConnectorDTO.builder()
            .jiraUrl("https://harness.atlassian.net/")
            .username("username")
            .passwordRef(SecretRefData.builder().decryptedValue(new char[] {'3', '4', 'f', '5', '1'}).build())
            .build();
    return JiraTaskNGParameters.builder().jiraConnectorDTO(jiraConnectorDTO);
  }
}

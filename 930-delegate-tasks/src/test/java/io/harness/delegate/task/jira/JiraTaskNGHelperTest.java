/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.GARVIT;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.jira.JiraActionNG;
import io.harness.rule.Owner;
import io.harness.security.encryption.SecretDecryptionService;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDC)
public class JiraTaskNGHelperTest extends CategoryTest {
  @Mock private JiraTaskNGHandler jiraTaskNGHandler;
  @Mock private SecretDecryptionService secretDecryptionService;

  @InjectMocks private JiraTaskNGHelper jiraTaskNGHelper;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldTValidateCredentials() {
    JiraTaskNGParameters params = setupMocksForAction(JiraActionNG.VALIDATE_CREDENTIALS);
    jiraTaskNGHelper.getJiraTaskResponse(params);
    verify(jiraTaskNGHandler).validateCredentials(params);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldGetProjects() {
    JiraTaskNGParameters params = setupMocksForAction(JiraActionNG.GET_PROJECTS);
    jiraTaskNGHelper.getJiraTaskResponse(params);
    verify(jiraTaskNGHandler).getProjects(params);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldGetStatuses() {
    JiraTaskNGParameters params = setupMocksForAction(JiraActionNG.GET_STATUSES);
    jiraTaskNGHelper.getJiraTaskResponse(params);
    verify(jiraTaskNGHandler).getStatuses(params);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldGetIssue() {
    JiraTaskNGParameters params = setupMocksForAction(JiraActionNG.GET_ISSUE);
    jiraTaskNGHelper.getJiraTaskResponse(params);
    verify(jiraTaskNGHandler).getIssue(params);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldGetIssueCreateMetadata() {
    JiraTaskNGParameters params = setupMocksForAction(JiraActionNG.GET_ISSUE_CREATE_METADATA);
    jiraTaskNGHelper.getJiraTaskResponse(params);
    verify(jiraTaskNGHandler).getIssueCreateMetadata(params);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldGetIssueUpdateMetadata() {
    JiraTaskNGParameters params = setupMocksForAction(JiraActionNG.GET_ISSUE_UPDATE_METADATA);
    jiraTaskNGHelper.getJiraTaskResponse(params);
    verify(jiraTaskNGHandler).getIssueUpdateMetadata(params);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldCreateIssue() {
    JiraTaskNGParameters params = setupMocksForAction(JiraActionNG.CREATE_ISSUE);
    jiraTaskNGHelper.getJiraTaskResponse(params);
    verify(jiraTaskNGHandler).createIssue(params);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldUpdateIssue() {
    JiraTaskNGParameters params = setupMocksForAction(JiraActionNG.UPDATE_ISSUE);
    jiraTaskNGHelper.getJiraTaskResponse(params);
    verify(jiraTaskNGHandler).updateIssue(params);
  }

  private JiraTaskNGParameters setupMocksForAction(JiraActionNG action) {
    JiraTaskNGResponse mockedResponse = JiraTaskNGResponse.builder().build();
    JiraTaskNGParameters params = JiraTaskNGParameters.builder().action(action).build();
    when(secretDecryptionService.decrypt(any(), any())).thenReturn(null);
    when(jiraTaskNGHandler.createIssue(params)).thenReturn(mockedResponse);
    return params;
  }
}

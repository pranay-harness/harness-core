/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.search;

import static io.harness.beans.WorkflowType.ORCHESTRATION;
import static io.harness.rule.OwnerRule.UTKARSH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.EnvironmentType;
import io.harness.beans.FeatureName;
import io.harness.category.element.FunctionalTests;
import io.harness.ff.FeatureFlagService;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.Owner;
import io.harness.testframework.framework.Retry;
import io.harness.testframework.framework.matchers.BooleanMatcher;
import io.harness.testframework.restutils.ApplicationRestUtils;
import io.harness.testframework.restutils.EnvironmentRestUtils;
import io.harness.testframework.restutils.SearchRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;

import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.search.entities.deployment.DeploymentSearchEntity;
import software.wings.search.framework.SearchResult;
import software.wings.search.framework.SearchResults;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class DeploymentSearchEntitySyncTest extends AbstractFunctionalTest {
  @Inject private FeatureFlagService featureFlagService;
  @Inject private MainConfiguration mainConfiguration;
  private static final Retry retry = new Retry(10, 5000);
  private final String APP_NAME = "SyncTestApplication" + System.currentTimeMillis();
  private final String ENVIRONMENT_NAME = "SyncTestEnvironment" + System.currentTimeMillis();
  private final String WORKFLOW_NAME = "SyncTestWorkflow" + System.currentTimeMillis();
  private Application application;
  private Environment environment;
  private Workflow workflow;
  private WorkflowExecution workflowExecution;

  @Before
  public void setUp() {
    if (isSearchDisabled()) {
      return;
    }

    application = new Application();
    application.setName(APP_NAME);
    application = ApplicationRestUtils.createApplication(bearerToken, getAccount(), application);
    assertThat(application).isNotNull();

    environment = Environment.Builder.anEnvironment()
                      .name(ENVIRONMENT_NAME)
                      .environmentType(EnvironmentType.PROD)
                      .appId(application.getUuid())
                      .build();
    environment = EnvironmentRestUtils.createEnvironment(bearerToken, getAccount(), application.getUuid(), environment);
    assertThat(environment).isNotNull();

    workflow = new Workflow();
    workflow.setAppId(application.getUuid());
    workflow.setEnvId(environment.getUuid());
    workflow.setName(WORKFLOW_NAME);
    workflow.setWorkflowType(ORCHESTRATION);

    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = new CanaryOrchestrationWorkflow();
    workflow.setOrchestrationWorkflow(canaryOrchestrationWorkflow);

    workflow = WorkflowRestUtils.createWorkflow(bearerToken, getAccount().getUuid(), application.getUuid(), workflow);
    assertThat(workflow).isNotNull();

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(workflow.getWorkflowType());
    executionArgs.setOrchestrationId(workflow.getUuid());

    workflowExecution = runWorkflow(bearerToken, workflow.getAppId(), workflow.getEnvId(), executionArgs);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(FunctionalTests.class)
  public void testDeploymentCRUDSync() {
    if (isSearchDisabled()) {
      return;
    }

    BooleanMatcher booleanMatcher = new BooleanMatcher();
    retry.executeWithRetry(this::isDeploymentInSearchResponse, booleanMatcher, true);
    assertThat(workflowExecution).isNotNull();
    log.info("New deployment with id {} and name {} synced.", workflowExecution.getUuid(), workflowExecution.getName());

    WorkflowRestUtils.deleteWorkflow(bearerToken, workflow.getUuid(), workflow.getAppId());
    EnvironmentRestUtils.deleteEnvironment(
        bearerToken, environment.getAppId(), environment.getAccountId(), environment.getUuid());
    ApplicationRestUtils.deleteApplication(bearerToken, application.getUuid(), application.getAccountId());
  }

  private boolean isDeploymentInSearchResponse() {
    boolean worklfowExecutionFound = false;

    SearchResults searchResults = SearchRestUtils.search(bearerToken, application.getAccountId(), workflow.getName());

    for (SearchResult deploymentSearchResult : searchResults.getSearchResults().get(DeploymentSearchEntity.TYPE)) {
      if (deploymentSearchResult.getId().equals(workflowExecution.getUuid())
          && deploymentSearchResult.getName().equals(workflowExecution.getName())) {
        worklfowExecutionFound = true;
        break;
      }
    }
    return worklfowExecutionFound;
  }

  private boolean isSearchDisabled() {
    return !featureFlagService.isEnabled(FeatureName.SEARCH_REQUEST, getAccount().getUuid())
        || !mainConfiguration.isSearchEnabled();
  }
}

package io.harness.functional.search;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.Owner;
import io.harness.testframework.framework.Retry;
import io.harness.testframework.framework.matchers.BooleanMatcher;
import io.harness.testframework.restutils.ApplicationRestUtils;
import io.harness.testframework.restutils.EnvironmentRestUtils;
import io.harness.testframework.restutils.SearchRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.FeatureName;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.Workflow;
import software.wings.search.entities.workflow.WorkflowSearchEntity;
import software.wings.search.framework.SearchResult;
import software.wings.search.framework.SearchResults;
import software.wings.service.intfc.FeatureFlagService;

@Slf4j
public class WorkflowSearchEntitySyncTest extends AbstractFunctionalTest {
  @Inject private FeatureFlagService featureFlagService;
  @Inject private MainConfiguration mainConfiguration;
  private static final Retry retry = new Retry(10, 5000);
  private final String APP_NAME = "SyncTestApplication" + System.currentTimeMillis();
  private final String ENVIRONMENT_NAME = "SyncTestEnvironment" + System.currentTimeMillis();
  private final String WORKFLOW_NAME = "SyncTestWorkflow" + System.currentTimeMillis();
  private final String EDITED_WORKFLOW_NAME = WORKFLOW_NAME + "_Edited";
  private Application application;
  private Environment environment;
  private Workflow workflow;

  @Before
  public void setUp() {
    if (isSearchDisabled()) {
      return;
    }

    application = new Application();
    application.setName(APP_NAME);
    application = ApplicationRestUtils.createApplication(bearerToken, getAccount(), application);
    assertThat(application).isNotNull();

    environment = new Environment();
    environment.setName(ENVIRONMENT_NAME);
    environment.setEnvironmentType(EnvironmentType.PROD);
    environment = EnvironmentRestUtils.createEnvironment(bearerToken, getAccount(), application.getUuid(), environment);
    assertThat(environment).isNotNull();

    workflow = new Workflow();
    workflow.setAppId(application.getUuid());
    workflow.setName(WORKFLOW_NAME);

    OrchestrationWorkflow orchestrationWorkflow = new CanaryOrchestrationWorkflow();
    workflow.setOrchestrationWorkflow(orchestrationWorkflow);

    workflow = WorkflowRestUtils.createWorkflow(bearerToken, getAccount().getUuid(), application.getUuid(), workflow);
    assertThat(workflow).isNotNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(FunctionalTests.class)
  public void testWorkflowCRUDSync() {
    if (isSearchDisabled()) {
      return;
    }

    BooleanMatcher booleanMatcher = new BooleanMatcher();
    retry.executeWithRetry(this ::isWorkflowInSearchResponse, booleanMatcher, true);
    logger.info("New workflow with id {} and name {} synced.", workflow.getUuid(), workflow.getName());

    workflow.setName(EDITED_WORKFLOW_NAME);
    workflow = WorkflowRestUtils.updateWorkflow(bearerToken, application.getAccountId(), workflow.getAppId(), workflow);

    assertThat(workflow).isNotNull();
    assertThat(workflow.getName()).isEqualTo(EDITED_WORKFLOW_NAME);

    retry.executeWithRetry(this ::isWorkflowInSearchResponse, booleanMatcher, true);
    logger.info("Workflow update with id {} and name {} synced.", workflow.getUuid(), workflow.getName());

    assertThat(WorkflowRestUtils.deleteWorkflow(bearerToken, workflow.getUuid(), workflow.getAppId())).isNull();

    retry.executeWithRetry(this ::isWorkflowInSearchResponse, booleanMatcher, false);
    logger.info("Workflow delete with id {} synced", workflow.getUuid());

    EnvironmentRestUtils.deleteEnvironment(
        bearerToken, application.getUuid(), application.getAccountId(), environment.getUuid());
    ApplicationRestUtils.deleteApplication(bearerToken, application.getUuid(), application.getAccountId());
  }

  private boolean isWorkflowInSearchResponse() {
    boolean worklfowFound = false;

    SearchResults searchResults = SearchRestUtils.search(bearerToken, application.getAccountId(), workflow.getName());

    for (SearchResult workflowSearchResult : searchResults.getSearchResults().get(WorkflowSearchEntity.TYPE)) {
      if (workflowSearchResult.getId().equals(workflow.getUuid())
          && workflowSearchResult.getName().equals(workflow.getName())) {
        worklfowFound = true;
        break;
      }
    }
    return worklfowFound;
  }

  private boolean isSearchDisabled() {
    return !featureFlagService.isGlobalEnabled(FeatureName.SEARCH)
        || !featureFlagService.isEnabled(FeatureName.SEARCH_REQUEST, getAccount().getUuid())
        || !mainConfiguration.isSearchEnabled();
  }
}
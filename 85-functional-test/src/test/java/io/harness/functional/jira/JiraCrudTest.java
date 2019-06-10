package io.harness.functional.jira;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.generator.EnvironmentGenerator.Environments.GENERIC_TEST;
import static io.harness.generator.SettingGenerator.Settings.HARNESS_JIRA;
import static io.harness.rule.OwnerRule.POOJA;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.sm.StateType.JIRA_CREATE_UPDATE;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.WorkflowUtils;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.SettingGenerator;
import io.harness.rule.OwnerRule.Owner;
import io.harness.testframework.restutils.WorkflowRestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.GraphNode;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.service.intfc.WorkflowExecutionService;

import java.util.Collections;

public class JiraCrudTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private SettingGenerator settingGenerator;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private WorkflowUtils workflowUtils;

  private Application application;

  private final Seed seed = new Seed(0);
  private Owners owners;

  @Before
  public void setUp() {
    owners = ownerManager.create();
    application = applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST);
    assertThat(application).isNotNull();
  }

  @Test
  @Owner(emails = POOJA)
  @Category(FunctionalTests.class)
  public void shouldCreateJiraStepinWorkflow() throws Exception {
    Environment environment = environmentGenerator.ensurePredefined(seed, owners, GENERIC_TEST);
    assertThat(environment).isNotNull();
    Workflow jiraWorkflow =
        workflowUtils.buildCanaryWorkflowPostDeploymentStep("Create JIRA", environment.getUuid(), getJiraCreateNode());

    // REST API.
    Workflow savedWorkflow =
        WorkflowRestUtils.createWorkflow(bearerToken, application.getAccountId(), application.getUuid(), jiraWorkflow);
    assertThat(savedWorkflow).isNotNull();

    // Test running the workflow
    WorkflowExecution workflowExecution =
        runWorkflow(bearerToken, application.getUuid(), environment.getUuid(), savedWorkflow.getUuid());
    assertThat(workflowExecution).isNotNull();
    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  private GraphNode getJiraCreateNode() {
    SettingAttribute jiraSetting = settingGenerator.ensurePredefined(seed, owners, HARNESS_JIRA);
    assertThat(jiraSetting).isNotNull();
    return GraphNode.builder()
        .id(generateUuid())
        .type(JIRA_CREATE_UPDATE.name())
        .name("Create Jira")
        .properties(ImmutableMap.<String, Object>builder()
                        .put("description", "test123")
                        .put("issueType", "Story")
                        .put("jiraAction", "CREATE_TICKET")
                        .put("jiraConnectorId", jiraSetting.getUuid())
                        .put("priority", "P1")
                        .put("project", "TJI")
                        .put("publishAsVar", true)
                        .put("summary", "test")
                        .put("sweepingOutputName", "Jiravar")
                        .put("sweepingOutputScope", "PIPELINE")
                        .put("labels", Collections.singletonList("demo"))
                        .build())
        .build();
  }
}

package io.harness.functional.trigger;

import static io.harness.rule.OwnerRule.SRINIVAS;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.trigger.Action.ActionType.WORKFLOW;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.ServiceGenerator.Services;
import io.harness.generator.WorkflowGenerator;
import io.harness.generator.WorkflowGenerator.Workflows;
import io.harness.generator.artifactstream.ArtifactStreamManager;
import io.harness.generator.artifactstream.ArtifactStreamManager.ArtifactStreams;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.testframework.framework.Setup;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.trigger.ArtifactCondition;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.TriggerArgs;
import software.wings.beans.trigger.WorkflowAction;

import javax.ws.rs.core.GenericType;

public class DeploymentTriggerFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private ArtifactStreamManager artifactStreamManager;

  Application application;

  final Seed seed = new Seed(0);
  Owners owners;

  @Before
  public void setUp() {
    owners = ownerManager.create();
    application = applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST);
    assertThat(application).isNotNull();
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(FunctionalTests.class)
  public void shouldCreateNewArtifactTriggerForWorkflow() {
    final Service service =
        owners.obtainService(() -> serviceGenerator.ensurePredefined(seed, owners, Services.GENERIC_TEST));
    assertThat(service).isNotNull();
    ArtifactStream artifactStream =
        artifactStreamManager.ensurePredefined(seed, owners, ArtifactStreams.HARNESS_SAMPLE_ECHO_WAR);
    assertThat(artifactStream).isNotNull();
    Workflow buildWorkflow = workflowGenerator.ensurePredefined(seed, owners, Workflows.BUILD_JENKINS);
    assertThat(buildWorkflow).isNotNull();
    GenericType<RestResponse<DeploymentTrigger>> triggerType = new GenericType<RestResponse<DeploymentTrigger>>() {

    };

    String name = "New Artifact Trigger" + System.currentTimeMillis();
    DeploymentTrigger trigger = DeploymentTrigger.builder()
                                    .action(WorkflowAction.builder()
                                                .triggerArgs(TriggerArgs.builder().build())
                                                .workflowId(buildWorkflow.getUuid())
                                                .build())
                                    .name(name)
                                    .appId(application.getAppId())
                                    .condition(ArtifactCondition.builder()
                                                   .artifactServerId("SETTING_ID")
                                                   .artifactStreamId(artifactStream.getUuid())
                                                   .build())
                                    .build();

    RestResponse<DeploymentTrigger> savedTriggerResponse = Setup.portal()
                                                               .auth()
                                                               .oauth2(bearerToken)
                                                               .queryParam("accountId", application.getAccountId())
                                                               .queryParam("appId", application.getUuid())
                                                               .body(trigger, ObjectMapperType.GSON)
                                                               .contentType(ContentType.JSON)
                                                               .post("/deployment-triggers")
                                                               .as(triggerType.getType());

    DeploymentTrigger savedTrigger = savedTriggerResponse.getResource();

    WorkflowAction workflowAction = (WorkflowAction) savedTrigger.getAction();
    assertThat(savedTrigger).isNotNull();
    assertThat(savedTrigger.getUuid()).isNotEmpty();
    assertThat(workflowAction.getWorkflowId()).isEqualTo(buildWorkflow.getUuid());
    assertThat(workflowAction.getActionType()).isEqualTo(WORKFLOW);

    // Get the saved trigger
    savedTriggerResponse = Setup.portal()
                               .auth()
                               .oauth2(bearerToken)
                               .queryParam("appId", application.getUuid())
                               .pathParam("triggerId", savedTrigger.getUuid())
                               .get("/deployment-triggers/{triggerId}")
                               .as(triggerType.getType());
    savedTrigger = savedTriggerResponse.getResource();
    workflowAction = (WorkflowAction) savedTrigger.getAction();
    assertThat(savedTrigger).isNotNull();
    assertThat(savedTrigger.getUuid()).isNotEmpty();
    assertThat(workflowAction.getWorkflowId()).isEqualTo(buildWorkflow.getUuid());
    assertThat(workflowAction.getActionType()).isEqualTo(WORKFLOW);

    // Delete the trigger
    Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("appId", application.getUuid())
        .pathParam("triggerId", savedTrigger.getUuid())
        .delete("/deployment-triggers/{triggerId}")
        .then()
        .statusCode(200);

    // Make sure that it is deleted
    savedTriggerResponse = Setup.portal()
                               .auth()
                               .oauth2(bearerToken)
                               .queryParam("appId", application.getUuid())
                               .pathParam("triggerId", savedTrigger.getUuid())
                               .get("/deployment-triggers/{triggerId}")
                               .as(triggerType.getType());
    assertThat(savedTriggerResponse.getResource()).isNull();
  }
}
package io.harness.functional.trigger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.WorkflowType.ORCHESTRATION;

import com.google.common.collect.ImmutableMap;
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
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.RestResponse;
import software.wings.beans.WebHookToken;
import software.wings.beans.Workflow;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.WebHookTriggerCondition;

import javax.ws.rs.core.GenericType;

public class TriggerFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private ServiceGenerator serviceGenerator;

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
  @Category(FunctionalTests.class)
  public void shouldCreateWebHookTriggerForWorkflow() {
    owners.obtainService(() -> serviceGenerator.ensurePredefined(seed, owners, Services.GENERIC_TEST));

    Workflow buildWorkflow = workflowGenerator.ensurePredefined(seed, owners, Workflows.BUILD_JENKINS);
    assertThat(buildWorkflow).isNotNull();
    GenericType<RestResponse<Trigger>> triggerType = new GenericType<RestResponse<Trigger>>() {

    };

    Trigger trigger = Trigger.builder()
                          .workflowId(buildWorkflow.getUuid())
                          .name("WebHook Trigger")
                          .appId(application.getAppId())
                          .workflowType(ORCHESTRATION)
                          .condition(WebHookTriggerCondition.builder()
                                         .webHookToken(WebHookToken.builder().build())
                                         .parameters(ImmutableMap.of("MyVar", "MyVal"))
                                         .build())
                          .build();

    RestResponse<Trigger> savedTriggerResponse = given()
                                                     .auth()
                                                     .oauth2(bearerToken)
                                                     .queryParam("accountId", application.getAccountId())
                                                     .queryParam("appId", application.getUuid())
                                                     .body(trigger, ObjectMapperType.GSON)
                                                     .contentType(ContentType.JSON)
                                                     .post("/triggers")
                                                     .as(triggerType.getType());

    Trigger savedTrigger = savedTriggerResponse.getResource();
    assertThat(savedTrigger).isNotNull();
    assertThat(savedTrigger.getUuid()).isNotEmpty();
    assertThat(savedTrigger.getWorkflowId()).isEqualTo(buildWorkflow.getUuid());
    assertThat(savedTrigger.getWorkflowType()).isEqualTo(ORCHESTRATION);

    // Get the saved workflow
    savedTriggerResponse = given()
                               .auth()
                               .oauth2(bearerToken)
                               .queryParam("appId", application.getUuid())
                               .pathParam("triggerId", savedTrigger.getUuid())
                               .get("/triggers/{triggerId}")
                               .as(triggerType.getType());
    savedTrigger = savedTriggerResponse.getResource();
    assertThat(savedTrigger).isNotNull();
    assertThat(savedTrigger.getUuid()).isNotEmpty();
    assertThat(savedTrigger.getWorkflowId()).isEqualTo(buildWorkflow.getUuid());
    assertThat(savedTrigger.getWorkflowType()).isEqualTo(ORCHESTRATION);

    // Delete the trigger
    given()
        .auth()
        .oauth2(bearerToken)
        .queryParam("appId", application.getUuid())
        .pathParam("triggerId", savedTrigger.getUuid())
        .delete("/triggers/{triggerId}")
        .then()
        .statusCode(200);

    // Make sure that it is deleted
    savedTriggerResponse = given()
                               .auth()
                               .oauth2(bearerToken)
                               .queryParam("appId", application.getUuid())
                               .pathParam("triggerId", savedTrigger.getUuid())
                               .get("/triggers/{triggerId}")
                               .as(triggerType.getType());
    assertThat(savedTriggerResponse.getResource()).isNull();
  }
}

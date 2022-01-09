/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.trigger;

import static io.harness.beans.WorkflowType.ORCHESTRATION;
import static io.harness.beans.WorkflowType.PIPELINE;
import static io.harness.generator.PipelineGenerator.Pipelines.BASIC;
import static io.harness.rule.OwnerRule.POOJA;
import static io.harness.rule.OwnerRule.SRINIVAS;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.EnvironmentGenerator.Environments;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.PipelineGenerator;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.ServiceGenerator.Services;
import io.harness.generator.WorkflowGenerator;
import io.harness.generator.WorkflowGenerator.Workflows;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.testframework.framework.Setup;

import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineExecution;
import software.wings.beans.WebHookToken;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.trigger.PipelineTriggerCondition;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.service.impl.WorkflowExecutionServiceImpl;
import software.wings.service.intfc.PipelineService;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import javax.ws.rs.core.GenericType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TriggerFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private WorkflowGenerator workflowGenerator;
  @Inject protected WorkflowExecutionServiceImpl workflowExecutionService;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private PipelineGenerator pipelineGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private PipelineService pipelineService;

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
  @Owner(developers = SRINIVAS, intermittent = true)
  @Category(FunctionalTests.class)
  public void shouldCreateWebHookTriggerForWorkflow() {
    owners.obtainService(() -> serviceGenerator.ensurePredefined(seed, owners, Services.GENERIC_TEST));

    Workflow buildWorkflow = workflowGenerator.ensurePredefined(seed, owners, Workflows.BUILD_JENKINS);
    assertThat(buildWorkflow).isNotNull();
    GenericType<RestResponse<Trigger>> triggerType = new GenericType<RestResponse<Trigger>>() {

    };

    String name = "WebHook Trigger " + System.currentTimeMillis();
    Trigger trigger = Trigger.builder()
                          .workflowId(buildWorkflow.getUuid())
                          .name(name)
                          .appId(application.getAppId())
                          .workflowType(ORCHESTRATION)
                          .condition(WebHookTriggerCondition.builder()
                                         .webHookToken(WebHookToken.builder().build())
                                         .parameters(ImmutableMap.of("MyVar", "MyVal"))
                                         .build())
                          .build();

    RestResponse<Trigger> savedTriggerResponse = Setup.portal()
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
    savedTriggerResponse = Setup.portal()
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
    Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("appId", application.getUuid())
        .pathParam("triggerId", savedTrigger.getUuid())
        .delete("/triggers/{triggerId}")
        .then()
        .statusCode(200);

    // Make sure that it is deleted
    savedTriggerResponse = Setup.portal()
                               .auth()
                               .oauth2(bearerToken)
                               .queryParam("appId", application.getUuid())
                               .pathParam("triggerId", savedTrigger.getUuid())
                               .get("/triggers/{triggerId}")
                               .as(triggerType.getType());
    assertThat(savedTriggerResponse.getResource()).isNull();
  }

  @Test
  @Owner(developers = POOJA, intermittent = true)
  @Category(FunctionalTests.class)
  public void testPipelineCompletionTrigger() {
    GenericType<RestResponse<Trigger>> triggerType = new GenericType<RestResponse<Trigger>>() {

    };
    owners.obtainService(() -> serviceGenerator.ensurePredefined(seed, owners, Services.GENERIC_TEST));
    Environment environment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);

    final Workflow postTriggerWorkflow = workflowGenerator.ensurePredefined(seed, owners, Workflows.BUILD_JENKINS);
    final Pipeline buildPipeline = pipelineGenerator.ensurePredefined(seed, owners, BASIC);
    assertThat(buildPipeline).isNotNull();

    String triggerName = "Test pipeline completion trigger";
    Trigger trigger = Trigger.builder()
                          .name(triggerName)
                          .condition(PipelineTriggerCondition.builder().pipelineId(buildPipeline.getUuid()).build())
                          .workflowId(postTriggerWorkflow.getUuid())
                          .appId(application.getAppId())
                          .workflowType(ORCHESTRATION)
                          .build();
    RestResponse<Trigger> savedTriggerResponse = Setup.portal()
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
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(PIPELINE);
    executionArgs.setPipelineId(buildPipeline.getUuid());

    resetCache(application.getUuid());
    resetCache(getAccount().getUuid());

    WorkflowExecution workflowExecution =
        runPipeline(bearerToken, application.getAppId(), environment.getUuid(), executionArgs);
    PipelineExecution pipelineExecution = workflowExecution.getPipelineExecution();
    pipelineExecution.setStatus(ExecutionStatus.SUCCESS);
    boolean status = workflowExecutionService.workflowExecutionsRunning(
        WorkflowType.ORCHESTRATION, application.getAppId(), postTriggerWorkflow.getUuid());

    // Execution should be running
    assertThat(status).isTrue();

    // Delete the trigger
    deleteTrigger(savedTrigger.getUuid(), application.getUuid());

    // Make sure that it is deleted
    Trigger deleteTrigger = getTrigger(savedTrigger.getUuid(), application.getUuid());

    assertThat(deleteTrigger).isNull();
  }

  public void deleteTrigger(String uuId, String appId) {
    Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("appId", appId)
        .pathParam("triggerId", uuId)
        .delete("/triggers/{triggerId}")
        .then()
        .statusCode(200);
  }

  public Trigger getTrigger(String uuId, String appId) {
    GenericType<RestResponse<Trigger>> triggerType = new GenericType<RestResponse<Trigger>>() {

    };
    RestResponse<Trigger> savedTriggerResponse = Setup.portal()
                                                     .auth()
                                                     .oauth2(bearerToken)
                                                     .queryParam("appId", appId)
                                                     .pathParam("triggerId", uuId)
                                                     .get("/triggers/{triggerId}")
                                                     .as(triggerType.getType());

    return savedTriggerResponse.getResource();
  }
}

/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.nas;

import static io.harness.beans.WorkflowType.ORCHESTRATION;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.generator.EnvironmentGenerator.Environments.GENERIC_TEST;
import static io.harness.rule.OwnerRule.AADITI;

import static software.wings.beans.trigger.ArtifactSelection.Type.WEBHOOK_VARIABLE;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.WorkflowUtils;
import io.harness.generator.AccountGenerator;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.InfrastructureDefinitionGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.SettingGenerator;
import io.harness.generator.WorkflowGenerator;
import io.harness.generator.artifactstream.ArtifactStreamManager;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.restutils.ArtifactStreamRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;

import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.ArtifactStreamMetadata;
import software.wings.beans.ArtifactVariable;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.ExecutionCredential.ExecutionType;
import software.wings.beans.SSHExecutionCredential;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WebHookRequest;
import software.wings.beans.WebHookToken;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.beans.trigger.ArtifactSelection;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.utils.RepositoryFormat;

import com.google.gson.Gson;
import com.google.inject.Inject;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.path.json.JsonPath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.GenericType;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class NASWorkflowExecutionTest extends AbstractFunctionalTest {
  private static final String ARTIFACT_STREAM_NAME = "nexus2-npm-metadataOnly-parameterized";
  @Inject private OwnerManager ownerManager;
  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private AccountGenerator accountGenerator;
  @Inject private ArtifactStreamManager artifactStreamManager;
  @Inject private SettingGenerator settingGenerator;
  @Inject private WorkflowUtils workflowUtils;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private InfrastructureDefinitionGenerator infrastructureDefinitionGenerator;

  private final Randomizer.Seed seed = new Randomizer.Seed(0);

  private OwnerManager.Owners owners;
  private Service service;
  private Environment environment;
  private InfrastructureDefinition infrastructureDefinition;
  Application application;
  Account account;
  SettingAttribute settingAttribute;
  Workflow workflow;
  ArtifactStream artifactStream;

  @Before
  public void setUp() {
    owners = ownerManager.create();

    application =
        applicationGenerator.ensurePredefined(seed, owners, ApplicationGenerator.Applications.FUNCTIONAL_TEST);
    assertThat(application).isNotNull();

    service = serviceGenerator.ensurePredefined(seed, owners, ServiceGenerator.Services.NAS_FUNCTIONAL_TEST);
    assertThat(service).isNotNull();

    account = owners.obtainAccount();
    if (account == null) {
      account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
    }
    settingAttribute =
        settingGenerator.ensurePredefined(seed, owners, SettingGenerator.Settings.HARNESS_NEXUS2_CONNECTOR);

    environment = environmentGenerator.ensurePredefined(seed, owners, GENERIC_TEST);
    assertThat(environment).isNotNull();
    infrastructureDefinition = infrastructureDefinitionGenerator.ensurePredefined(
        seed, owners, InfrastructureDefinitionGenerator.InfrastructureDefinitions.PHYSICAL_SSH_TEST);

    // create parameterized nexus npm artifact stream
    final String accountId = service.getAccountId();
    artifactStream = artifactStreamManager.ensurePredefined(seed, owners,
        ArtifactStreamManager.ArtifactStreams.NEXUS2_NPM_METADATA_ONLY_PARAMETERIZED,
        NexusArtifactStream.builder()
            .appId(application.getUuid())
            .serviceId(service.getUuid())
            .autoPopulate(false)
            .metadataOnly(true)
            .name(ARTIFACT_STREAM_NAME)
            .sourceName(settingAttribute.getName())
            .repositoryFormat(RepositoryFormat.npm.name())
            .jobname("${repo}")
            .packageName("${package}")
            .settingId(settingAttribute.getUuid())
            .build());
    assertThat(artifactStream).isNotNull();
    service.setArtifactStreamIds(new ArrayList<>(Arrays.asList(artifactStream.getUuid())));
    resetCache(accountId);

    // create workflow
    Workflow basicWorkflow =
        workflowUtils.createBasicWorkflowWithShellScript("basic-with-params", service, infrastructureDefinition);
    workflow =
        WorkflowRestUtils.createWorkflow(bearerToken, application.getAccountId(), application.getUuid(), basicWorkflow);
    assertThat(workflow).isNotNull();
  }

  @Test
  @Owner(developers = AADITI)
  @Category({FunctionalTests.class})
  public void executeBasicWorkflowWithParameterizedArtifactStream() {
    Map<String, Object> runtimeValues = new HashMap<>();
    runtimeValues.put("repo", "npm-internal");
    runtimeValues.put("package", "npm-app1");
    runtimeValues.put("buildNo", "1.0.0");
    executeBasicWorkflowAndCheckStatus(runtimeValues, ExecutionStatus.SUCCESS.name(), false);
  }

  @Test
  @Owner(developers = AADITI)
  @Category({FunctionalTests.class})
  public void executeBasicWorkflowWithIncorrectParameters() {
    Map<String, Object> runtimeValues = new HashMap<>();
    runtimeValues.put("repo", "npm-internal1");
    runtimeValues.put("package", "npm-app1");
    runtimeValues.put("buildNo", "1.0.0");
    executeBasicWorkflowAndCheckStatus(runtimeValues, ExecutionStatus.FAILED.name(), true);
  }

  private void executeBasicWorkflowAndCheckStatus(
      Map<String, Object> runtimeValues, String executionStatus, boolean validateMessage) {
    // get artifact variables from deployment metadata for given workflow
    List<ArtifactVariable> artifactVariables = workflowGenerator.getArtifactVariablesFromDeploymentMetadataForWorkflow(
        application.getUuid(), workflow.getUuid());
    if (isNotEmpty(artifactVariables)) {
      for (ArtifactVariable artifactVariable : artifactVariables) {
        if (artifactVariable.getAllowedList().contains(artifactStream.getUuid())) {
          artifactVariable.setArtifactStreamMetadata(ArtifactStreamMetadata.builder()
                                                         .artifactStreamId(artifactStream.getUuid())
                                                         .runtimeValues(runtimeValues)
                                                         .build());
        }
      }
    }

    // execute workflow
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(WorkflowType.ORCHESTRATION);
    executionArgs.setExecutionCredential(
        SSHExecutionCredential.Builder.aSSHExecutionCredential().withExecutionType(ExecutionType.SSH).build());
    executionArgs.setOrchestrationId(workflow.getUuid());
    executionArgs.setArtifactVariables(artifactVariables);
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("appId", application.getUuid());
    if (environment.getUuid() != null) {
      queryParams.put("envId", environment.getUuid());
    }

    JsonPath savedWorkflowExecutionResponse = Setup.portal()
                                                  .auth()
                                                  .oauth2(bearerToken)
                                                  .queryParams(queryParams)
                                                  .contentType(ContentType.JSON)
                                                  .body(executionArgs, ObjectMapperType.GSON)
                                                  .post("/executions")
                                                  .jsonPath();
    String workflowExecutionId = (String) ((HashMap) savedWorkflowExecutionResponse.get("resource")).get("uuid");
    assertThat(workflowExecutionId).isNotNull();

    Awaitility.await()
        .atMost(300, TimeUnit.SECONDS)
        .pollInterval(5, TimeUnit.SECONDS)
        .until(()
                   -> Setup.portal()
                          .auth()
                          .oauth2(bearerToken)
                          .queryParam("appId", application.getUuid())
                          .get("/executions/" + workflowExecutionId)
                          .jsonPath()
                          .<String>getJsonObject("resource.status")
                          .equals(executionStatus));
    if (validateMessage) {
      String failureMessage = Setup.portal()
                                  .auth()
                                  .oauth2(bearerToken)
                                  .queryParam("appId", application.getUuid())
                                  .get("/executions/" + workflowExecutionId)
                                  .jsonPath()
                                  .<String>getJsonObject("resource.message");
      assertThat(failureMessage)
          .isEqualTo("Error collecting build for artifact source " + ARTIFACT_STREAM_NAME + ".\n");
    }
  }

  @Test
  @Owner(developers = AADITI)
  @Category({FunctionalTests.class})
  public void shouldExecuteTrigger() {
    GenericType<RestResponse<Trigger>> triggerType = new GenericType<RestResponse<Trigger>>() {

    };
    String name = "WebHook Trigger " + System.currentTimeMillis();
    Trigger trigger =
        Trigger.builder()
            .workflowId(workflow.getUuid())
            .name(name)
            .appId(application.getAppId())
            .workflowType(ORCHESTRATION)
            .artifactSelections(asList(ArtifactSelection.builder()
                                           .type(WEBHOOK_VARIABLE)
                                           .serviceId(service.getUuid())
                                           .artifactStreamId(artifactStream.getUuid())
                                           .build()))
            .condition(WebHookTriggerCondition.builder().webHookToken(WebHookToken.builder().build()).build())
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
    assertThat(savedTrigger.getWorkflowId()).isEqualTo(workflow.getUuid());
    assertThat(savedTrigger.getWorkflowType()).isEqualTo(ORCHESTRATION);
    String json = ((WebHookTriggerCondition) savedTrigger.getCondition()).getWebHookToken().getPayload();
    Gson gson = new Gson();
    json = json.replace("Test NAS Service_BUILD_NUMBER_PLACE_HOLDER", "1.1.0");
    json = json.replace("REPO_PLACE_HOLDER", "npm-internal");
    json = json.replace("PACKAGE_PLACE_HOLDER", "npm-app1");
    WebHookRequest webHookRequest = gson.fromJson(json, WebHookRequest.class);

    JsonPath response =
        Setup.portal()
            .body(webHookRequest, ObjectMapperType.GSON)
            .pathParam("webHookToken",
                ((WebHookTriggerCondition) savedTrigger.getCondition()).getWebHookToken().getWebHookToken())
            .contentType(ContentType.JSON)
            .post("/webhooks/{webHookToken}")
            .jsonPath();

    assertThat(response.getObject("status", String.class)).isEqualTo("RUNNING");
  }

  @After
  public void tearDown() {
    WorkflowRestUtils.deleteWorkflow(bearerToken, workflow.getUuid(), application.getUuid());
    ArtifactStreamRestUtils.deleteArtifactStream(bearerToken, artifactStream.getUuid(), application.getUuid());
  }
}

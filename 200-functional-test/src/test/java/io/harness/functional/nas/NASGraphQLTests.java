/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.nas;

import static io.harness.generator.EnvironmentGenerator.Environments.FUNCTIONAL_TEST;
import static io.harness.rule.OwnerRule.AADITI;

import static software.wings.sm.StateType.ENV_STATE;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.FeatureName;
import io.harness.category.element.FunctionalTests;
import io.harness.ff.FeatureFlagService;
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
import io.harness.rule.Owner;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.utils.TestUtils;
import io.harness.testframework.restutils.ArtifactStreamRestUtils;
import io.harness.testframework.restutils.GraphQLRestUtils;
import io.harness.testframework.restutils.PipelineRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;

import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.utils.RepositoryFormat;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class NASGraphQLTests extends AbstractFunctionalTest {
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
  @Inject private FeatureFlagService featureFlagService;

  private final Randomizer.Seed seed = new Randomizer.Seed(0);

  private OwnerManager.Owners owners;
  private Service service;
  private Environment environment;
  private InfrastructureDefinition infrastructureDefinition;
  Application application;
  Account account;
  SettingAttribute settingAttribute;
  ArtifactStream artifactStream;
  Workflow workflow;
  Pipeline pipeline;

  @Before
  public void setUp() {
    owners = ownerManager.create();
    application =
        applicationGenerator.ensurePredefined(seed, owners, ApplicationGenerator.Applications.FUNCTIONAL_TEST);
    assertThat(application).isNotNull();
    if (!featureFlagService.isEnabled(FeatureName.GRAPHQL_DEV, application.getAccountId())) {
      featureFlagService.enableAccount(FeatureName.GRAPHQL_DEV, application.getAccountId());
    }
    service = serviceGenerator.ensurePredefined(seed, owners, ServiceGenerator.Services.NAS_FUNCTIONAL_TEST);
    assertThat(service).isNotNull();
    account = owners.obtainAccount();
    if (account == null) {
      account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
    }
    settingAttribute =
        settingGenerator.ensurePredefined(seed, owners, SettingGenerator.Settings.HARNESS_NEXUS2_CONNECTOR);
    environment = environmentGenerator.ensurePredefined(seed, owners, FUNCTIONAL_TEST);
    assertThat(environment).isNotNull();
    infrastructureDefinition = infrastructureDefinitionGenerator.ensurePredefined(
        seed, owners, InfrastructureDefinitionGenerator.InfrastructureDefinitions.PHYSICAL_SSH_TEST);

    final String accountId = service.getAccountId();
    resetCache(accountId);
    // create parameterized nexus nuget artifact stream
    artifactStream = artifactStreamManager.ensurePredefined(seed, owners,
        ArtifactStreamManager.ArtifactStreams.NEXUS2_NUGET_METADATA_ONLY_PARAMETERIZED,
        NexusArtifactStream.builder()
            .appId(application.getUuid())
            .serviceId(service.getUuid())
            .autoPopulate(false)
            .metadataOnly(true)
            .name("nexus2-nuget-metadataOnly-parameterized")
            .sourceName(settingAttribute.getName())
            .repositoryFormat(RepositoryFormat.nuget.name())
            .jobname("${repository}")
            .packageName("${packageName}")
            .settingId(settingAttribute.getUuid())
            .build());
    assertThat(artifactStream).isNotNull();
    service.setArtifactStreamIds(new ArrayList<>(Arrays.asList(artifactStream.getUuid())));
    resetCache(accountId);
    // Create basic workflow and add to pipeline
    Workflow basicWorkflow =
        workflowUtils.createBasicWorkflowWithShellScript("basic-with-params", service, infrastructureDefinition);
    workflow =
        WorkflowRestUtils.createWorkflow(bearerToken, application.getAccountId(), application.getUuid(), basicWorkflow);
    assertThat(workflow).isNotNull();
    List<PipelineStage> pipelineStages =
        asList(getPipelineStage(environment.getUuid(), workflow.getUuid(), "Dev stage1"));

    Pipeline samplePipeline = Pipeline.builder()
                                  .appId(application.getUuid())
                                  .name("pipeline1" + System.currentTimeMillis())
                                  .description("Sample Pipeline")
                                  .pipelineStages(pipelineStages)
                                  .accountId(application.getAccountId())
                                  .build();

    pipeline =
        PipelineRestUtils.createPipeline(application.getAppId(), samplePipeline, getAccount().getUuid(), bearerToken);
    assertThat(pipeline).isNotNull();
    assertThat(pipeline.getUuid()).isNotNull();
  }

  @Test
  @Owner(developers = AADITI)
  @Category(FunctionalTests.class)
  public void shouldTriggerPipeline() {
    String mutation = getGraphqlQueryForWorkflowExecution("123", pipeline.getUuid(), application.getAppId(),
        service.getName(), artifactStream.getName(), "nuget-hosted", "NuGet.Sample.Package", "1.0.0.0");
    Map<Object, Object> response =
        GraphQLRestUtils.executeGraphQLQuery(bearerToken, application.getAccountId(), mutation);

    assertThat(response).isNotEmpty();
    assertThat(response.get("startExecution")).isNotNull();
    Map<String, Object> executionData = (Map<String, Object>) response.get("startExecution");
    assertThat(executionData.get("clientMutationId")).isEqualTo("123");
    assertThat(executionData.get("execution")).isNotNull();
    String workflowExecutionId = (String) ((Map<String, Object>) executionData.get("execution")).get("id");
    Awaitility.await()
        .atMost(120, TimeUnit.SECONDS)
        .pollInterval(5, TimeUnit.SECONDS)
        .until(()
                   -> Setup.portal()
                          .auth()
                          .oauth2(bearerToken)
                          .queryParam("appId", application.getUuid())
                          .get("/executions/" + workflowExecutionId)
                          .jsonPath()
                          .<String>getJsonObject("resource.status")
                          .equals(ExecutionStatus.SUCCESS.name()));
  }

  private String getGraphqlQueryForWorkflowExecution(String clientMutationId, String pipelineId, String appId,
      String serviceName, String artifactSourceName, String repository, String packageName, String buildNo) {
    String serviceInputQuery =
        $GQL(/*[{
name: "%s"
artifactValueInput: {
valueType: PARAMETERIZED_ARTIFACT_SOURCE
parameterizedArtifactSource: {
buildNumber: "%s",
artifactSourceName: "%s",
parameterValueInputs:[{
              name:"repository"
              value: "%s"
            },
            {
              name: "packageName"
              value: "%s"
            }
]
}}}]*/ serviceName, buildNo, artifactSourceName, repository, packageName);

    String mutationInputQuery = $GQL(/*
{
entityId: "%s",
applicationId: "%s",
executionType: PIPELINE,
serviceInputs: %s,
clientMutationId: "%s"
}*/ pipelineId, appId, serviceInputQuery, clientMutationId);

    return $GQL(/*
mutation{
startExecution(input:%s) {
clientMutationId
execution {
 status
 id
}
}
}*/ mutationInputQuery);
  }

  @Test
  @Owner(developers = AADITI)
  @Category(FunctionalTests.class)
  public void getExecutionInputsPipeline() {
    String query = getGraphqlQueryForExecutionInputs(pipeline.getUuid(), application.getAppId());
    Map<Object, Object> response = GraphQLRestUtils.executeGraphQLQuery(bearerToken, application.getAccountId(), query);

    assertThat(response).isNotEmpty();
    assertThat(response.get("executionInputs")).isNotNull();
    Map<String, Object> executionInputs = (Map<String, Object>) response.get("executionInputs");
    assertThat(executionInputs.get("serviceInputs")).isNotNull();
    List<Object> serviceInputs = (List<Object>) executionInputs.get("serviceInputs");

    assertThat(serviceInputs.size()).isEqualTo(1);
    Map<String, Object> serviceInput0 = (Map<String, Object>) serviceInputs.get(0);
    assertThat(serviceInput0.get("id")).isEqualTo(service.getUuid());
    List<Map> list = (List<Map>) serviceInput0.get("artifactSources");
    for (Map map : list) {
      if (map.get("name").equals(artifactStream.getName())) {
        List<String> parameters = (List<String>) map.get("parameters");
        assertThat(parameters.size()).isEqualTo(2);
        assertThat(parameters).containsAll(asList("repository", "packageName"));
      }
    }
  }

  private String getGraphqlQueryForExecutionInputs(String pipelineId, String appId) {
    return $GQL(/*
query {
  executionInputs(entityId: "%s"
    applicationId: "%s"
    executionType: PIPELINE
){
  serviceInputs{
    id
    name
    artifactType
    artifactSources{
     id
     name
      ... on NexusArtifactSource {
        parameters
      }
    }
  }
  }
}*/ pipelineId, appId);
  }

  private PipelineStage getPipelineStage(String envId, String workflowId, String name) {
    return PipelineStage.builder()
        .name(TestUtils.generateRandomUUID())
        .pipelineStageElements(asList(PipelineStageElement.builder()
                                          .uuid(TestUtils.generateRandomUUID())
                                          .name(name)
                                          .type(ENV_STATE.name())
                                          .properties(ImmutableMap.of("envId", envId, "workflowId", workflowId))
                                          .build()))
        .build();
  }

  @After
  public void tearDown() {
    PipelineRestUtils.deletePipeline(application.getUuid(), pipeline.getUuid(), bearerToken);
    WorkflowRestUtils.deleteWorkflow(bearerToken, workflow.getUuid(), application.getUuid());
    ArtifactStreamRestUtils.deleteArtifactStream(bearerToken, artifactStream.getUuid(), application.getUuid());
  }
}

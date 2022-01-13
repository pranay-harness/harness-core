/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.graphQLAPIs.pipeline;

import static io.harness.functional.WorkflowUtils.getTemplateExpressionsForEnv;
import static io.harness.functional.WorkflowUtils.getTemplateExpressionsForInfraDefinition;
import static io.harness.functional.WorkflowUtils.getTemplateExpressionsForService;
import static io.harness.generator.EnvironmentGenerator.Environments.GENERIC_TEST;
import static io.harness.rule.OwnerRule.POOJA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.FeatureName;
import io.harness.category.element.FunctionalTests;
import io.harness.ff.FeatureFlagService;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.WorkflowUtils;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.InfrastructureDefinitionGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.ServiceGenerator;
import io.harness.rule.Owner;
import io.harness.testframework.framework.utils.PipelineUtils;
import io.harness.testframework.restutils.GraphQLRestUtils;
import io.harness.testframework.restutils.PipelineRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;

import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureType;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowService;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GetPipelineByIdAndNameFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private InfrastructureDefinitionGenerator infrastructureDefinitionGenerator;
  @Inject private WorkflowUtils workflowUtils;

  private Application application;
  private Pipeline templatisedPipeline;
  private Service service;
  private Environment environment;
  private InfrastructureDefinition infrastructureDefinition;
  private String workflowId;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private WorkflowService workflowService;
  @Inject private PipelineService pipelineService;

  final Randomizer.Seed seed = new Randomizer.Seed(0);
  OwnerManager.Owners owners;

  @Before
  public void setUp() {
    owners = ownerManager.create();
    application = applicationGenerator.ensurePredefined(seed, owners, ApplicationGenerator.Applications.GENERIC_TEST);
    assertThat(application).isNotNull();

    if (!featureFlagService.isEnabled(FeatureName.GRAPHQL_DEV, application.getAccountId())) {
      featureFlagService.enableAccount(FeatureName.GRAPHQL_DEV, application.getAccountId());
    }

    service = serviceGenerator.ensureK8sTest(seed, owners, "k8s-service");
    environment = environmentGenerator.ensurePredefined(seed, owners, GENERIC_TEST);
    infrastructureDefinition = infrastructureDefinitionGenerator.ensurePredefined(
        seed, owners, InfrastructureType.GCP_KUBERNETES_ENGINE, bearerToken);

    resetCache(application.getAccountId());
    createTemplatisedPipeline();
  }

  private void createTemplatisedPipeline() {
    Workflow workflow =
        workflowUtils.getRollingK8sWorkflow("gcp-k8s-templatized-pooja-", service, infrastructureDefinition);
    Workflow savedWorkflow =
        WorkflowRestUtils.createWorkflow(bearerToken, application.getAccountId(), application.getUuid(), workflow);
    assertThat(savedWorkflow).isNotNull();

    // Templatize env and infra of  the workflow
    savedWorkflow.setTemplateExpressions(Arrays.asList(getTemplateExpressionsForEnv(),
        getTemplateExpressionsForService(), getTemplateExpressionsForInfraDefinition("${InfraDefinition_Kubernetes}")));

    Workflow templatizedWorkflow =
        WorkflowRestUtils.updateWorkflow(bearerToken, application.getAccountId(), application.getUuid(), savedWorkflow);
    assertThat(templatizedWorkflow.isEnvTemplatized()).isTrue();
    assertThat(templatizedWorkflow.isTemplatized()).isTrue();
    workflowId = templatizedWorkflow.getUuid();

    String pipelineName = "GraphQLAPI Test - " + System.currentTimeMillis();

    Pipeline pipeline = new Pipeline();
    pipeline.setName(pipelineName);
    pipeline.setDescription("description");

    Pipeline createdPipeline =
        PipelineRestUtils.createPipeline(application.getAppId(), pipeline, getAccount().getUuid(), bearerToken);
    assertThat(createdPipeline).isNotNull();

    ImmutableMap<String, String> workflowVariables = ImmutableMap.<String, String>builder()
                                                         .put("Environment", "${env}")
                                                         .put("Service", "${service}")
                                                         .put("InfraDefinition_Kubernetes", "${infra}")
                                                         .build();

    List<PipelineStage> pipelineStages = new ArrayList<>();
    PipelineStage executionStage =
        PipelineUtils.prepareExecutionStage(environment.getUuid(), templatizedWorkflow.getUuid(), workflowVariables);
    pipelineStages.add(executionStage);
    createdPipeline.setPipelineStages(pipelineStages);

    templatisedPipeline = PipelineRestUtils.updatePipeline(application.getAppId(), createdPipeline, bearerToken);
  }

  @Test
  @Owner(developers = POOJA, intermittent = true)
  @Category(FunctionalTests.class)
  public void getTemplatisedPipelineWithVarsById() {
    String query = getGraphQLQueryById(templatisedPipeline.getUuid());
    Map<Object, Object> response = GraphQLRestUtils.executeGraphQLQuery(bearerToken, application.getAccountId(), query);
    assertThat(response).isNotEmpty();
    validateResponse(response, "pipeline");
  }

  private void validateResponse(Map<Object, Object> response, String apiName) {
    assertThat(response).isNotEmpty();
    assertThat(response.get(apiName)).isNotNull();
    Map<String, Object> pipeline = (Map<String, Object>) response.get(apiName);
    assertThat(pipeline.get("name")).isEqualTo(templatisedPipeline.getName());
    assertThat(pipeline.get("id")).isEqualTo(templatisedPipeline.getUuid());
    assertThat(pipeline.get("pipelineVariables")).isNotNull();
    List<HashMap<String, Object>> pipelineVariables = (List<HashMap<String, Object>>) pipeline.get("pipelineVariables");
    assertThat(pipelineVariables.size()).isEqualTo(3);
    Map<String, Object> envVar = pipelineVariables.get(0);
    assertThat(envVar.get("name")).isEqualTo("env");
    assertThat(envVar.get("type")).isEqualTo("Environment");
    assertThat(envVar.get("required")).isEqualTo(Boolean.TRUE);

    Map<String, Object> serviceVar = pipelineVariables.get(1);
    assertThat(serviceVar.get("name")).isEqualTo("service");
    assertThat(serviceVar.get("type")).isEqualTo("Service");
    assertThat(serviceVar.get("required")).isEqualTo(Boolean.TRUE);

    Map<String, Object> infraVar = pipelineVariables.get(2);
    assertThat(infraVar.get("name")).isEqualTo("infra");
    assertThat(infraVar.get("type")).isEqualTo("Infrastructure definition");
    assertThat(infraVar.get("required")).isEqualTo(Boolean.TRUE);
  }

  private String getGraphQLQueryById(String pipelineId) {
    return $GQL(/*
query{
pipeline(pipelineId: "%s"){
name
id
pipelineVariables {
  name
  type
  required
}
}
}*/ pipelineId);
  }

  @Test
  @Owner(developers = POOJA, intermittent = true)
  @Category(FunctionalTests.class)
  public void getTemplatisedPipelineWithVarsByName() {
    String query = getGraphQLQueryByName(templatisedPipeline.getName(), templatisedPipeline.getAppId());
    Map<Object, Object> response = GraphQLRestUtils.executeGraphQLQuery(bearerToken, application.getAccountId(), query);
    assertThat(response).isNotEmpty();
    validateResponse(response, "pipelineByName");
  }

  private String getGraphQLQueryByName(String pipelineName, String appId) {
    return $GQL(/*
query{
pipelineByName(pipelineName: "%s", applicationId: "%s"){
name
id
pipelineVariables {
  name
  type
  required
}
}
}*/ pipelineName, appId);
  }

  @After
  public void destroy() {
    pipelineService.deletePipeline(application.getUuid(), templatisedPipeline.getUuid());
    workflowService.deleteWorkflow(application.getUuid(), workflowId);
  }
}

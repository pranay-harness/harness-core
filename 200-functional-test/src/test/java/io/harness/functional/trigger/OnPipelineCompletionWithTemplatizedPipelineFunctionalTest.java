/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.trigger;

import static io.harness.functional.WorkflowUtils.getTemplateExpressionsForEnv;
import static io.harness.functional.WorkflowUtils.getTemplateExpressionsForInfraDefinition;
import static io.harness.functional.WorkflowUtils.getTemplateExpressionsForService;
import static io.harness.generator.EnvironmentGenerator.Environments.GENERIC_TEST;
import static io.harness.rule.OwnerRule.MILAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
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
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.CDC)
public class OnPipelineCompletionWithTemplatizedPipelineFunctionalTest extends AbstractFunctionalTest {
  private final Randomizer.Seed seed = new Randomizer.Seed(0);
  private OwnerManager.Owners owners;

  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private WorkflowUtils workflowUtils;
  @Inject private InfrastructureDefinitionGenerator infrastructureDefinitionGenerator;
  @Inject private WorkflowService workflowService;
  @Inject private PipelineService pipelineService;

  private Application application;
  private Service service;
  private Environment environment;
  private InfrastructureDefinition infrastructureDefinition;
  private Pipeline templatisedPipeline;
  private Workflow templatizedWorkflow;

  @Before
  public void setUp() {
    owners = ownerManager.create();

    application = applicationGenerator.ensurePredefined(seed, owners, ApplicationGenerator.Applications.GENERIC_TEST);
    if (!featureFlagService.isEnabled(FeatureName.GRAPHQL_DEV, application.getAccountId())) {
      featureFlagService.enableAccount(FeatureName.GRAPHQL_DEV, application.getAccountId());
    }

    assertThat(application).isNotNull();
    service = serviceGenerator.ensureK8sTest(seed, owners, "k8s-service");
    environment = environmentGenerator.ensurePredefined(seed, owners, GENERIC_TEST);
    infrastructureDefinition = infrastructureDefinitionGenerator.ensurePredefined(
        seed, owners, InfrastructureType.GCP_KUBERNETES_ENGINE, bearerToken);

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

    templatizedWorkflow =
        WorkflowRestUtils.updateWorkflow(bearerToken, application.getAccountId(), application.getUuid(), savedWorkflow);
    assertThat(templatizedWorkflow.isEnvTemplatized()).isTrue();
    assertThat(templatizedWorkflow.isTemplatized()).isTrue();

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
  @Owner(developers = MILAN, intermittent = true)
  @Category(FunctionalTests.class)
  public void shouldCRUDTrigger() {
    // CREATE
    String clientMutationId = "1234";
    String artifactSourceId = service.getArtifactStreamIds().get(0);
    ImmutableMap<String, String> workflowVariables = ImmutableMap.<String, String>builder()
                                                         .put("env", environment.getName())
                                                         .put("service", service.getName())
                                                         .put("infra", infrastructureDefinition.getName())
                                                         .build();

    String mutation = getGraphQLQueryForTriggerCreation(clientMutationId, application.getAppId(),
        templatisedPipeline.getUuid(), service.getUuid(), artifactSourceId, workflowVariables);
    Map<Object, Object> response =
        GraphQLRestUtils.executeGraphQLQuery(bearerToken, application.getAccountId(), mutation);
    assertThat(response).isNotEmpty();

    Map<String, Object> createTrigger = (Map<String, Object>) response.get("createTrigger");
    assertThat(createTrigger).isNotNull();

    assertThat(createTrigger.get("clientMutationId")).isEqualTo("1234");

    Map<String, Object> trigger = (Map<String, Object>) createTrigger.get("trigger");
    assertThat(trigger).isNotNull();
    String triggerId = (String) trigger.get("id");

    // UPDATE
    String triggerName = "updatedTriggerName3";
    mutation = getGraphQLQueryForTriggerUpdate(clientMutationId, triggerId, triggerName, application.getAppId(),
        templatisedPipeline.getUuid(), service.getUuid(), artifactSourceId, workflowVariables);
    response = GraphQLRestUtils.executeGraphQLQuery(bearerToken, application.getAccountId(), mutation);
    assertThat(response).isNotEmpty();

    Map<String, Object> updateTrigger = (Map<String, Object>) response.get("updateTrigger");
    assertThat(updateTrigger).isNotNull();

    assertThat(updateTrigger.get("clientMutationId")).isEqualTo(clientMutationId);

    trigger = (Map<String, Object>) updateTrigger.get("trigger");
    assertThat(trigger).isNotNull();
    assertThat((String) trigger.get("name")).isEqualTo(triggerName);

    // GET
    String query = getGraphQLQueryForTriggerRetrieval((String) trigger.get("id"));
    response = GraphQLRestUtils.executeGraphQLQuery(bearerToken, application.getAccountId(), query);

    assertThat(response).isNotEmpty();

    trigger = (Map<String, Object>) response.get("trigger");
    assertThat(trigger).isNotNull();

    assertThat(trigger.get("id")).isNotNull();
    assertThat(trigger.get("name")).isNotNull();

    Map<String, Object> condition = (Map<String, Object>) trigger.get("condition");
    assertThat(condition).isNotNull();
    assertThat(condition.get("triggerConditionType")).isNotNull();
    assertThat(condition.get("artifactSourceId")).isEqualTo(artifactSourceId);
    assertThat(condition.get("artifactFilter")).isEqualTo("filter");
    assertThat(condition.get("regex")).isEqualTo(true);

    Map<String, Object> action = (Map<String, Object>) trigger.get("action");
    assertThat(action).isNotNull();
    assertThat(action.get("pipelineId")).isEqualTo(templatisedPipeline.getUuid());
    assertThat(action.get("pipelineName")).isEqualTo(templatisedPipeline.getName());

    List<Map<String, Object>> artifactSelections = (List<Map<String, Object>>) action.get("artifactSelections");
    assertThat((String) artifactSelections.get(0).get("serviceId")).isEqualTo(service.getUuid());
    assertThat((String) artifactSelections.get(0).get("serviceName")).isEqualTo(service.getName());

    assertThat(action.get("variables")).isNotNull();
    List<Map<String, String>> variables = (List<Map<String, String>>) action.get("variables");
    assertThat(variables.size()).isEqualTo(3);

    // DELETE
    mutation = getGraphQLQueryForTriggerDeletion(clientMutationId, application.getAppId(), triggerId);
    response = GraphQLRestUtils.executeGraphQLQuery(bearerToken, application.getAccountId(), mutation);

    assertThat(response).isNotEmpty();

    Map<String, Object> deleteTrigger = (Map<String, Object>) response.get("deleteTrigger");
    assertThat(deleteTrigger).isNotNull();

    assertThat(deleteTrigger.get("clientMutationId")).isEqualTo(clientMutationId);
  }

  private String getGraphQLQueryForTriggerCreation(String clientMutationId, String appId, String pipelineId,
      String serviceId, String artifactSourceId, Map<String, String> workflowVariables) {
    String variableInputsQuery = getVariableInputsQuery(workflowVariables);
    return $GQL(/*
mutation {
createTrigger(input: {
clientMutationId: "%s",
applicationId: "%s",
name: "trigger3",
condition: {
conditionType: ON_NEW_ARTIFACT,
artifactConditionInput: {
artifactSourceId: "%s",
artifactFilter: "filter",
regex: true
}
},
action: {
entityId: "%s",
executionType: PIPELINE,
artifactSelections: {
artifactFilter: "filter",
artifactSelectionType: FROM_TRIGGERING_ARTIFACT,
artifactSourceId: "%s",
pipelineId: "%s",
regex: false,
serviceId: "%s",
},
variables: %s
}
}) {
clientMutationId,
trigger {
id,
action {
... on PipelineAction {
 variables {
 name,
 value
 }
}
}
}
}
}*/ clientMutationId, appId, artifactSourceId, pipelineId, artifactSourceId, pipelineId, serviceId,
        variableInputsQuery);
  }

  private String getGraphQLQueryForTriggerUpdate(String clientMutationId, String triggerId, String triggerName,
      String appId, String pipelineId, String serviceId, String artifactSourceId,
      Map<String, String> workflowVariables) {
    String variableInputsQuery = getVariableInputsQuery(workflowVariables);
    return $GQL(/*
mutation {
updateTrigger(input: {
triggerId: "%s",
clientMutationId: "%s",
applicationId: "%s",
name: "%s",
condition: {
  conditionType: ON_NEW_ARTIFACT,
artifactConditionInput: {
artifactSourceId: "%s",
artifactFilter: "filter",
regex: true
}
},
action: {
  entityId: "%s",
  executionType: PIPELINE,
  artifactSelections: {
    artifactFilter: "filter",
    artifactSelectionType: FROM_TRIGGERING_ARTIFACT,
    artifactSourceId: "%s",
    pipelineId: "%s",
    regex: false,
    serviceId: "%s",
  },
 variables: %s
}
}) {
clientMutationId,
trigger {
  id,
  name
}
}
}*/ triggerId, clientMutationId, appId, triggerName, artifactSourceId, pipelineId, artifactSourceId, pipelineId,
        serviceId, variableInputsQuery);
  }

  private String getGraphQLQueryForTriggerRetrieval(String triggerId) {
    return $GQL(/*
query {
trigger(triggerId: "%s") {
id,
name,
action {
  ...on PipelineAction {
    pipelineId,
    pipelineName,
    artifactSelections {
      serviceId,
      serviceName
    },
    variables {
      name,
      value
    }
  }
},
condition {
  triggerConditionType,
  ...on OnNewArtifact {
    artifactSourceId,
    artifactSourceName,
    regex,
    artifactFilter,
    triggerConditionType
  }
}
}
}*/ triggerId);
  }

  private String getGraphQLQueryForTriggerDeletion(String clientMutationId, String applicationId, String triggerId) {
    return $GQL(/*
mutation {
  deleteTrigger(input: {
    clientMutationId: "%s",
    applicationId: "%s",
    triggerId: "%s"
  }) {
    clientMutationId
  }
}*/ clientMutationId, applicationId, triggerId);
  }

  @NotNull
  private String getVariableInputsQuery(Map<String, String> variableValues) {
    List<String> variableInputs = new ArrayList<>();
    for (Map.Entry<String, String> entry : variableValues.entrySet()) {
      String queryVariableInput = $GQL(/*{
name: "%s"
variableValue: {
    value: "%s"
    type: NAME
    }}*/ entry.getKey(), entry.getValue());
      variableInputs.add(queryVariableInput);
    }
    return "[" + String.join(",", variableInputs) + "]";
  }

  @After
  public void destroy() {
    pipelineService.deletePipeline(application.getUuid(), templatisedPipeline.getUuid());
    workflowService.deleteWorkflow(application.getUuid(), templatizedWorkflow.getUuid());
  }
}

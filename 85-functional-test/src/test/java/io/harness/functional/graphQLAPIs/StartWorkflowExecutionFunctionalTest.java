package io.harness.functional.graphQLAPIs;

import static io.harness.functional.WorkflowUtils.getTemplateExpressionsForEnv;
import static io.harness.functional.WorkflowUtils.getTemplateExpressionsForInfraDefinition;
import static io.harness.functional.WorkflowUtils.getTemplateExpressionsForService;
import static io.harness.generator.EnvironmentGenerator.Environments.GENERIC_TEST;
import static io.harness.rule.OwnerRule.POOJA;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.WorkflowUtils;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.InfrastructureDefinitionGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.ServiceGenerator;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.ArtifactRestUtils;
import io.harness.testframework.restutils.GraphQLRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;
import lombok.Data;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.FeatureName;
import software.wings.beans.InfrastructureType;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.Artifact;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.intfc.FeatureFlagService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class StartWorkflowExecutionFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private InfrastructureDefinitionGenerator infrastructureDefinitionGenerator;
  @Inject private WorkflowUtils workflowUtils;
  @Inject private FeatureFlagService featureFlagService;

  private Application application;
  private Service service;
  private Environment environment;
  private InfrastructureDefinition infrastructureDefinition;

  private final Randomizer.Seed seed = new Randomizer.Seed(0);
  private OwnerManager.Owners owners;

  @Before
  public void setUp() {
    owners = ownerManager.create();

    application = applicationGenerator.ensurePredefined(seed, owners, ApplicationGenerator.Applications.GENERIC_TEST);
    if (!featureFlagService.isEnabled(FeatureName.GRAPHQL_DEV, application.getAccountId())) {
      featureFlagService.enableAccount(FeatureName.GRAPHQL_DEV, application.getAccountId());
    }
    if (!featureFlagService.isEnabled(FeatureName.GRAPHQL, application.getAccountId())) {
      featureFlagService.enableAccount(FeatureName.GRAPHQL, application.getAccountId());
    }

    assertThat(application).isNotNull();
    service = serviceGenerator.ensureK8sTest(seed, owners, "k8s-service");
    environment = environmentGenerator.ensurePredefined(seed, owners, GENERIC_TEST);
    infrastructureDefinition = infrastructureDefinitionGenerator.ensurePredefined(
        seed, owners, InfrastructureType.GCP_KUBERNETES_ENGINE, bearerToken);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(FunctionalTests.class)
  public void shouldTriggerTemplatisedWorkflow() {
    Workflow workflow = workflowUtils.getRollingK8sWorkflow("GraphQLAPI-test-", service, infrastructureDefinition);
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

    ImmutableMap<String, String> workflowVariables =
        ImmutableMap.<String, String>builder()
            .put("Environment", environment.getName())
            .put("Service", service.getName())
            .put("InfraDefinition_Kubernetes", infrastructureDefinition.getName())
            .build();
    Artifact artifact = getArtifact(service, service.getAppId());

    String mutation = getGraphqlQueryForWorkflow("123", application.getAppId(), templatizedWorkflow.getUuid(),
        workflowVariables, artifact.getUuid(), service.getName());
    Map<Object, Object> response =
        GraphQLRestUtils.executeMutationGraphQLQuery(bearerToken, application.getAccountId(), mutation);

    assertThat(response).isNotEmpty();
    assertThat(response.get("startExecution")).isNotNull();
    Map<String, Object> executionData = (Map<String, Object>) response.get("startExecution");
    assertThat(executionData.get("clientMutationId")).isEqualTo("123");
    assertThat(executionData.get("execution")).isNotNull();
  }

  private Artifact getArtifact(Service service, String appId) {
    return ArtifactRestUtils.waitAndFetchArtifactByArtfactStream(
        bearerToken, appId, service.getArtifactStreamIds().get(0), 0);
  }

  private String getGraphqlQueryForWorkflow(String clientMutationId, String appId, String workflowId,
      Map<String, String> variableValues, String artifactId, String serviceName) {
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
    String variableInputsQuery = "[" + String.join(",", variableInputs) + "]";
    String serviceInputQuery =
        $GQL(/*[{
name: "%s"
artifactValueInput: {
valueType: ARTIFACT_ID
artifactId: {
artifactId: "%s"
}}}]*/ serviceName, artifactId);

    String mutationInputQuery = $GQL(/*
{
entityId: "%s",
applicationId: "%s",
executionType: WORKFLOW,
variableInputs: %s,
serviceInputs: %s,
clientMutationId: "%s"
}*/ workflowId, appId, variableInputsQuery, serviceInputQuery, clientMutationId);

    return $GQL(/*
mutation{
startExecution(input:%s) {
clientMutationId
execution {
 status
}
}
}*/ mutationInputQuery);
  }

  @Data
  private static class QLStartExecutionResult {
    private String clientMutationId;
    private QLExecutionResult execution;
  }

  @Data
  private static class QLExecutionResult {
    private String status;
  }
}

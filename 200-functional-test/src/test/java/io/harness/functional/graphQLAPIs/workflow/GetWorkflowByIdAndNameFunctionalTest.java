/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.graphQLAPIs.workflow;

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
import io.harness.testframework.restutils.GraphQLRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;

import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureType;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.intfc.WorkflowService;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GetWorkflowByIdAndNameFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private InfrastructureDefinitionGenerator infrastructureDefinitionGenerator;
  @Inject private WorkflowUtils workflowUtils;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private WorkflowService workflowService;

  private Application application;
  private Workflow templatizedWorkflow;
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

    assertThat(application).isNotNull();
    service = serviceGenerator.ensureK8sTest(seed, owners, "k8s-service");
    environment = environmentGenerator.ensurePredefined(seed, owners, GENERIC_TEST);
    infrastructureDefinition = infrastructureDefinitionGenerator.ensurePredefined(
        seed, owners, InfrastructureType.GCP_KUBERNETES_ENGINE, bearerToken);

    Workflow workflow = workflowUtils.getRollingK8sWorkflow("Pipeline-Test", service, infrastructureDefinition);
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
  }

  @Test
  @Owner(developers = POOJA, intermittent = true)
  @Category(FunctionalTests.class)
  public void shouldGetTemplatisedWorkflowWithVariableById() {
    String query = getGraphQLQueryById(templatizedWorkflow.getUuid());
    Map<Object, Object> response = GraphQLRestUtils.executeGraphQLQuery(bearerToken, application.getAccountId(), query);
    assertThat(response).isNotEmpty();
    validateResponse(response, "workflow");
  }

  @Test
  @Owner(developers = POOJA, intermittent = true)
  @Category({FunctionalTests.class})
  public void shouldGetTemplatisedWorkflowWithVariableByName() {
    String query = getGraphQLQueryByName(templatizedWorkflow.getName(), templatizedWorkflow.getAppId());
    Map<Object, Object> response = GraphQLRestUtils.executeGraphQLQuery(bearerToken, application.getAccountId(), query);
    assertThat(response).isNotEmpty();
    validateResponse(response, "workflowByName");
  }

  private void validateResponse(Map<Object, Object> response, String apiName) {
    assertThat(response).isNotEmpty();
    assertThat(response.get(apiName)).isNotNull();
    Map<String, Object> workflow = (Map<String, Object>) response.get(apiName);
    assertThat(workflow.get("name")).isEqualTo(templatizedWorkflow.getName());
    assertThat(workflow.get("id")).isEqualTo(templatizedWorkflow.getUuid());
    assertThat(workflow.get("workflowVariables")).isNotNull();
    List<HashMap<String, Object>> workflowVariables = (List<HashMap<String, Object>>) workflow.get("workflowVariables");
    assertThat(workflowVariables.size()).isEqualTo(3);
    Map<String, Object> envVar = workflowVariables.get(0);
    assertThat(envVar.get("name")).isEqualTo("Environment");
    assertThat(envVar.get("type")).isEqualTo("Environment");
    assertThat(envVar.get("required")).isEqualTo(Boolean.TRUE);

    Map<String, Object> serviceVar = workflowVariables.get(1);
    assertThat(serviceVar.get("name")).isEqualTo("Service");
    assertThat(serviceVar.get("type")).isEqualTo("Service");
    assertThat(serviceVar.get("required")).isEqualTo(Boolean.TRUE);

    Map<String, Object> infraVar = workflowVariables.get(2);
    assertThat(infraVar.get("name")).isEqualTo("InfraDefinition_Kubernetes");
    assertThat(infraVar.get("type")).isEqualTo("Infrastructure definition");
    assertThat(infraVar.get("required")).isEqualTo(Boolean.TRUE);
  }

  private String getGraphQLQueryById(String workflowId) {
    return $GQL(/*
query{
workflow(workflowId: "%s"){
name
id
workflowVariables {
  name
  type
  required
}
}
}*/ workflowId);
  }

  private String getGraphQLQueryByName(String workflowName, String appId) {
    return $GQL(/*
query{
workflowByName(workflowName: "%s", applicationId: "%s"){
name
id
workflowVariables {
  name
  type
  required
}
}
}*/ workflowName, appId);
  }

  @After
  public void destroy() {
    workflowService.deleteWorkflow(application.getUuid(), templatizedWorkflow.getUuid());
  }
}

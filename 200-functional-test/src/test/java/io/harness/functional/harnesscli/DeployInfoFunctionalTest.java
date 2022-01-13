/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.harnesscli;

import static io.harness.functional.harnesscli.DeployFunctionalTest.getPipelineWithTwoStages;
import static io.harness.functional.harnesscli.DeployFunctionalTest.getTemplateExpressions;
import static io.harness.rule.OwnerRule.ROHIT;

import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.VariableType.TEXT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.CliFunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.WorkflowUtils;
import io.harness.generator.InfrastructureDefinitionGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.ServiceGenerator;
import io.harness.rule.Owner;
import io.harness.testframework.framework.utils.PipelineUtils;
import io.harness.testframework.restutils.PipelineRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;

import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.Service;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.infra.InfrastructureDefinition;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class DeployInfoFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private WorkflowUtils workflowUtils;
  @Inject private InfrastructureDefinitionGenerator infrastructureDefinitionGenerator;
  @Inject HarnesscliHelper harnesscliHelper;

  private final Seed seed = new Seed(0);

  private Owners owners;
  private String appId;
  private String accountId;
  private String artifactId;
  private Service service;
  private InfrastructureDefinition infrastructureDefinition;

  @Before
  public void setUp() throws IOException {
    owners = ownerManager.create();
    service = serviceGenerator.ensureK8sTest(seed, owners, "k8s-service");
    service = serviceGenerator.ensureK8sTest(seed, owners, "k8s-service");
    appId = service.getAppId();
    accountId = getAccount().getUuid();
    infrastructureDefinition =
        infrastructureDefinitionGenerator.ensurePredefined(seed, owners, "GCP_KUBERNETES", bearerToken);
    harnesscliHelper.loginToCLI();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(CliFunctionalTests.class)
  @Ignore("Will be enabled along with InfraDefs Feature Flag")
  public void deployInfoForTemplateWorkflow() throws IOException {
    Workflow rollingWorkflow =
        workflowUtils.createRollingWorkflow("Test-Rolling-Template-CLI-Deployment", service, infrastructureDefinition);
    Workflow testWorkflow = WorkflowRestUtils.createWorkflow(bearerToken, accountId, appId, rollingWorkflow);
    testWorkflow.setTemplateExpressions(getTemplateExpressions());
    WorkflowRestUtils.updateWorkflow(bearerToken, accountId, appId, testWorkflow);
    String workflowId = testWorkflow.getUuid();

    String command = "harness deploy info --application " + appId + " --workflow " + workflowId;
    List<String> deployInfoOutput = harnesscliHelper.executeCLICommand(command);

    assertThat(deployInfoOutput.get(2).contains("SERVICE")).isTrue();
    assertThat(deployInfoOutput.get(2).contains("Mandatory")).isTrue();
    assertThat(deployInfoOutput.get(3).contains("Infra_Def")).isTrue();
    assertThat(deployInfoOutput.get(3).contains("Mandatory")).isTrue();

    log.info("Deleting the workflow");
    WorkflowRestUtils.deleteWorkflow(bearerToken, workflowId, appId);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(CliFunctionalTests.class)
  @Ignore("Will be enabled along with InfraDefs Feature Flag")
  public void deployInfoForUserVariablesInWorkflow() throws IOException {
    Workflow rollingWorkflow =
        workflowUtils.createRollingWorkflow("Test-Rolling-Template-CLI-Deployment", service, infrastructureDefinition);
    Workflow workflow = WorkflowRestUtils.createWorkflow(bearerToken, accountId, appId, rollingWorkflow);
    String workflowId = workflow.getUuid();
    List<Variable> userVariables = Arrays.asList(getOptionalVariable(), getRequiredVariable());
    WorkflowRestUtils.updateUserVariables(bearerToken, accountId, appId, workflowId, userVariables);

    String command = "harness deploy info --application " + appId + " --workflow " + workflowId;
    List<String> deployInfoOutput = harnesscliHelper.executeCLICommand(command);

    assertThat(deployInfoOutput.get(2).contains("optionalVar")).isTrue();
    assertThat(deployInfoOutput.get(2).contains("Optional")).isTrue();
    assertThat(deployInfoOutput.get(3).contains("requiredVar")).isTrue();
    assertThat(deployInfoOutput.get(3).contains("Mandatory")).isTrue();
    assertThat(deployInfoOutput.get(7).contains(service.getName())).isTrue();
    assertThat(deployInfoOutput.get(7).contains(service.getUuid())).isTrue();

    log.info("Deleting the workflow");
    WorkflowRestUtils.deleteWorkflow(bearerToken, workflowId, appId);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(CliFunctionalTests.class)
  @Ignore("Will be enabled along with InfraDefs Feature Flag")
  public void deployInfoTemplatePipeline() throws IOException {
    Pipeline pipeline = getPipelineWithTwoStages(getPipelineStageList(), appId, accountId);
    String pipelineId = pipeline.getUuid();

    String command = "harness deploy info --application " + appId + " --pipeline " + pipelineId;
    List<String> deployInfoOutput = harnesscliHelper.executeCLICommand(command);

    assertThat(deployInfoOutput.get(2).contains("SERVICE")).isTrue();
    assertThat(deployInfoOutput.get(2).contains("Mandatory")).isTrue();
    assertThat(deployInfoOutput.get(3).contains("Infra_Def")).isTrue();
    assertThat(deployInfoOutput.get(3).contains("Mandatory")).isTrue();

    log.info("Deleting the workflow");
    PipelineRestUtils.deletePipeline(bearerToken, pipeline.getUuid(), appId);
  }

  private Variable getOptionalVariable() {
    return aVariable().type(TEXT).name("optionalVar").mandatory(false).value("").build();
  }

  private Variable getRequiredVariable() {
    return aVariable().type(TEXT).name("requiredVar").mandatory(true).value("").build();
  }

  private List<PipelineStage> getPipelineStageList() {
    List<PipelineStage> pipelineStageList = new ArrayList<>();
    Workflow rollingWorkflowBeforeTemplatising =
        workflowUtils.createRollingWorkflow("Test-deployInfo", service, infrastructureDefinition);
    Workflow testWorkflow =
        WorkflowRestUtils.createWorkflow(bearerToken, accountId, appId, rollingWorkflowBeforeTemplatising);
    testWorkflow.setTemplateExpressions(getTemplateExpressions());
    Workflow templateRollingWorkflow = WorkflowRestUtils.updateWorkflow(bearerToken, accountId, appId, testWorkflow);
    String templateRollingWorkflowId = templateRollingWorkflow.getUuid();

    Map<String, String> variables = new HashMap<>();
    variables.put("service", "${service}");
    variables.put("Infra_Def", "${Infra_Def}");
    PipelineStage executionStage =
        PipelineUtils.prepareExecutionStage(infrastructureDefinition.getEnvId(), templateRollingWorkflowId, variables);
    pipelineStageList.add(executionStage);

    executionStage = PipelineUtils.prepareApprovalStage(getAccount(), bearerToken, "");
    pipelineStageList.add(executionStage);
    return pipelineStageList;
  }
}

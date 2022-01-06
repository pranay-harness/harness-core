/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.terragrunt;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.beans.WorkflowType.ORCHESTRATION;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.TATHAGAT;

import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;
import static software.wings.sm.StepType.TERRAGRUNT_DESTROY;
import static software.wings.sm.StepType.TERRAGRUNT_PROVISION;

import static io.grpc.netty.shaded.io.netty.util.internal.StringUtil.EMPTY_STRING;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.InfrastructureProvisionerGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.SettingGenerator;
import io.harness.generator.WorkflowGenerator;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.WorkflowRestUtils;

import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.GraphNode;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TerragruntInfrastructureProvisioner;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDP)
public class TerragruntFunctionalTest extends AbstractFunctionalTest {
  private static final String WORKFLOW_NAME_PREFIX = "Terragrunt workflow ";
  private static final String PROVISIONER_NAME_PREFIX = "Terragrunt infrastructure provisioner";
  private static final String TG_PLAN_STEP_NAME = "Terragrunt Provision Plan";
  private static final String TG_APPLY_STEP_NAME = "Terragrunt Provision Apply";
  private static final String SHELL_SCRIPT_STEP_NAME = "Shell Script";
  private static final String RESOURCE_PATH = "200-functional-test/src/test/resources/io/harness/functional/terraform";
  private static final String SCRIPT_NAME_APPLY = "CheckTerraformApplyPlanJsonScript";
  private static final String SCRIPT_NAME_DESTROY = "CheckTerraformDestroyPlanJsonScript";
  private static final String TG_PLAN_DESTROY_STEP_NAME = "Terragrunt Destroy Plan";
  private static final String TG_DESTROY_STEP_NAME = "Terragrunt Destroy";
  private static final String PATH_TO_MODULE = "prod-no-var-required";

  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private SettingGenerator settingGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private WorkflowService workflowService;
  @Inject private InfrastructureProvisionerGenerator infrastructureProvisionerGenerator;

  final Randomizer.Seed seed = new Randomizer.Seed(0);
  OwnerManager.Owners owners;
  Application application;
  SettingAttribute gitConnector;
  Environment environment;

  @Before
  public void setUp() {
    owners = ownerManager.create();

    gitConnector = settingGenerator.ensurePredefined(seed, owners, SettingGenerator.Settings.TERRAFORM_MAIN_GIT_REPO);
    assertThat(gitConnector).isNotNull();

    application = owners.obtainApplication(
        () -> applicationGenerator.ensurePredefined(seed, owners, ApplicationGenerator.Applications.GENERIC_TEST));
    assertThat(application).isNotNull();

    environment = owners.obtainEnvironment(
        () -> environmentGenerator.ensurePredefined(seed, owners, EnvironmentGenerator.Environments.GENERIC_TEST));
    assertThat(environment).isNotNull();
    logManagerFeatureFlags(application.getAccountId());
  }

  // marking intermittant as terragrunt is not install
  @Test
  @Owner(developers = TATHAGAT, intermittent = true)
  @Category(FunctionalTests.class)
  public void createTerragruntPlanForApply() throws IOException {
    InfrastructureProvisioner terragruntProvisioner = createTerragruntProvisioner(true);

    Workflow workflow = createCanaryWorkflow(environment.getUuid(), "RunPlanOnly");
    workflow = createAndAssertWorkflow(workflow);
    workflowService.updatePreDeployment(application.getAppId(), workflow.getUuid(),
        addPreDeploymentRunPlanOnlyPhaseStep(terragruntProvisioner, false, false, false));

    WorkflowExecution workflowExecution =
        runWorkflow(bearerToken, application.getUuid(), environment.getUuid(), getExecutionArgs(workflow));
    logStateExecutionInstanceErrors(workflowExecution);
    getFailedWorkflowExecutionLogs(workflowExecution);
    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = TATHAGAT, intermittent = true)
  @Category(FunctionalTests.class)
  public void applyApprovedPlanThenDestroy() throws IOException {
    InfrastructureProvisioner terragruntProvisioner = createTerragruntProvisioner(true);

    Workflow workflow = createCanaryWorkflow(environment.getUuid(), "ApprovedPlan");
    workflow = createAndAssertWorkflow(workflow);
    workflowService.updatePreDeployment(
        application.getAppId(), workflow.getUuid(), addPreDeploymentPhaseStep(terragruntProvisioner, true, false));
    workflowService.updatePostDeployment(
        application.getAppId(), workflow.getUuid(), addPostDeploymentPhaseStep(terragruntProvisioner, false));

    WorkflowExecution workflowExecution =
        runWorkflow(bearerToken, application.getUuid(), environment.getUuid(), getExecutionArgs(workflow));
    logStateExecutionInstanceErrors(workflowExecution);
    getFailedWorkflowExecutionLogs(workflowExecution);
    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = TATHAGAT, intermittent = true)
  @Category(FunctionalTests.class)
  public void createTerragruntPlanForDestroy() throws IOException {
    InfrastructureProvisioner terragruntProvisioner = createTerragruntProvisioner(true);

    Workflow workflow = createCanaryWorkflow(environment.getUuid(), "Destroy");
    workflow = createAndAssertWorkflow(workflow);
    workflowService.updatePostDeployment(application.getAppId(), workflow.getUuid(),
        addPostDeploymentRunPlanOnlyPhaseStep(terragruntProvisioner, false));

    WorkflowExecution workflowExecution =
        runWorkflow(bearerToken, application.getUuid(), environment.getUuid(), getExecutionArgs(workflow));
    logStateExecutionInstanceErrors(workflowExecution);
    getFailedWorkflowExecutionLogs(workflowExecution);
    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = TATHAGAT, intermittent = true)
  @Category(FunctionalTests.class)
  public void workflowWithCommitId() throws IOException {
    InfrastructureProvisioner terragruntProvisioner = createTerragruntProvisioner(false);

    Workflow workflow = createCanaryWorkflow(environment.getUuid(), "WithCommitId");
    workflow = createAndAssertWorkflow(workflow);
    workflowService.updatePreDeployment(
        application.getAppId(), workflow.getUuid(), addPreDeploymentPhaseStep(terragruntProvisioner, true, false));
    workflowService.updatePostDeployment(
        application.getAppId(), workflow.getUuid(), addPostDeploymentPhaseStep(terragruntProvisioner, false));

    WorkflowExecution workflowExecution =
        runWorkflow(bearerToken, application.getUuid(), environment.getUuid(), getExecutionArgs(workflow));
    logStateExecutionInstanceErrors(workflowExecution);
    getFailedWorkflowExecutionLogs(workflowExecution);
    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = TATHAGAT, intermittent = true)
  @Category(FunctionalTests.class)
  public void workflowWithEnvVars() throws IOException {
    InfrastructureProvisioner terragruntProvisioner = createTerragruntProvisioner(true);

    Workflow workflow = createCanaryWorkflow(environment.getUuid(), "WithEnvVars");
    workflow = createAndAssertWorkflow(workflow);
    workflowService.updatePreDeployment(
        application.getAppId(), workflow.getUuid(), addPreDeploymentPhaseStep(terragruntProvisioner, true, true));
    workflowService.updatePostDeployment(
        application.getAppId(), workflow.getUuid(), addPostDeploymentPhaseStep(terragruntProvisioner, true));

    WorkflowExecution workflowExecution =
        runWorkflow(bearerToken, application.getUuid(), environment.getUuid(), getExecutionArgs(workflow));
    logStateExecutionInstanceErrors(workflowExecution);
    getFailedWorkflowExecutionLogs(workflowExecution);
    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = TATHAGAT, intermittent = true)
  @Category(FunctionalTests.class)
  public void createTerragruntRunAllPlanOnly() throws IOException {
    InfrastructureProvisioner terragruntProvisioner = createTerragruntProvisioner(true);

    Workflow workflow = createCanaryWorkflow(environment.getUuid(), "RunAllPlanOnly");
    workflow = createAndAssertWorkflow(workflow);
    workflowService.updatePreDeployment(application.getAppId(), workflow.getUuid(),
        addPreDeploymentRunPlanOnlyPhaseStep(terragruntProvisioner, false, false, true));

    WorkflowExecution workflowExecution =
        runWorkflow(bearerToken, application.getUuid(), environment.getUuid(), getExecutionArgs(workflow));
    logStateExecutionInstanceErrors(workflowExecution);
    getFailedWorkflowExecutionLogs(workflowExecution);
    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  private Workflow createCanaryWorkflow(String envId, String workflowNameSuffix) {
    return aWorkflow()
        .name(WORKFLOW_NAME_PREFIX + workflowNameSuffix + System.currentTimeMillis())
        .appId(application.getUuid())
        .envId(envId)
        .workflowType(WorkflowType.ORCHESTRATION)
        .orchestrationWorkflow(aCanaryOrchestrationWorkflow().build())
        .build();
  }

  private PhaseStep addPreDeploymentPhaseStep(InfrastructureProvisioner infrastructureProvisioner,
      boolean exportPlanToApplyStep, boolean addEnvVars) throws IOException {
    PhaseStep phaseStep =
        addPreDeploymentRunPlanOnlyPhaseStep(infrastructureProvisioner, exportPlanToApplyStep, addEnvVars, false);

    GraphNode applyStep = createApplyStep(infrastructureProvisioner);

    phaseStep.getSteps().add(applyStep);
    return phaseStep;
  }

  private PhaseStep addPreDeploymentRunPlanOnlyPhaseStep(InfrastructureProvisioner infrastructureProvisioner,
      boolean exportPlanToApplyStep, boolean addEnvVars, boolean runAll) throws IOException {
    // Terragrunt Provision
    GraphNode provisionStep =
        createProvisionStep(infrastructureProvisioner, exportPlanToApplyStep, addEnvVars, false, runAll);

    // Shell Script
    GraphNode shellScript = createShellScriptStep(false);

    return aPhaseStep(PhaseStepType.PRE_DEPLOYMENT, "Pre-Deployment")
        .addStep(provisionStep)
        .addStep(shellScript)
        .build();
  }

  private GraphNode createProvisionStep(InfrastructureProvisioner infrastructureProvisioner,
      boolean exportPlanToApplyStep, boolean addEnvVars, boolean inheritApprovedPlan, boolean runAll) {
    return GraphNode.builder()
        .id(generateUuid())
        .name(TG_PLAN_STEP_NAME)
        .type(TERRAGRUNT_PROVISION.name())
        .properties(ImmutableMap.<String, Object>builder()
                        .put("name", TG_PLAN_STEP_NAME)
                        .put("provisionerId", infrastructureProvisioner.getUuid())
                        .put("inheritApprovedPlan", inheritApprovedPlan)
                        .put("runPlanOnly", true)
                        .put("runAll", runAll)
                        .put("exportPlanToApplyStep", exportPlanToApplyStep)
                        .put("pathToModule", PATH_TO_MODULE)
                        .put("variables", getVars(addEnvVars))
                        .put("environmentVariables", addEnvVars ? getEnvVars() : emptyList())
                        .build())
        .build();
  }

  private GraphNode createApplyStep(InfrastructureProvisioner infrastructureProvisioner) {
    return GraphNode.builder()
        .id(generateUuid())
        .name(TG_APPLY_STEP_NAME)
        .type(TERRAGRUNT_PROVISION.name())
        .properties(ImmutableMap.<String, Object>builder()
                        .put("name", TG_APPLY_STEP_NAME)
                        .put("provisionerId", infrastructureProvisioner.getUuid())
                        .put("inheritApprovedPlan", true)
                        .put("runPlanOnly", false)
                        .put("exportPlanToApplyStep", false)
                        .put("pathToModule", PATH_TO_MODULE)
                        .build())
        .build();
  }

  private GraphNode createShellScriptStep(boolean isDestroy) throws IOException {
    String shellScript = readFileContent(isDestroy ? SCRIPT_NAME_DESTROY : SCRIPT_NAME_APPLY, RESOURCE_PATH);
    return GraphNode.builder()
        .id(generateUuid())
        .name(SHELL_SCRIPT_STEP_NAME)
        .type(StateType.SHELL_SCRIPT.name())
        .properties(ImmutableMap.<String, Object>builder()
                        .put("scriptType", "BASH")
                        .put("scriptString", shellScript)
                        .put("timeoutMillis", 60000)
                        .put("executeOnDelegate", true)
                        .build())
        .build();
  }

  private GraphNode createDestroyStep(
      InfrastructureProvisioner infrastructureProvisioner, boolean runPlanOnly, boolean addEnvVars) {
    return GraphNode.builder()
        .id(generateUuid())
        .name(runPlanOnly ? TG_PLAN_DESTROY_STEP_NAME : TG_DESTROY_STEP_NAME)
        .type(TERRAGRUNT_DESTROY.name())
        .properties(ImmutableMap.<String, Object>builder()
                        .put("name", runPlanOnly ? TG_PLAN_DESTROY_STEP_NAME : TG_DESTROY_STEP_NAME)
                        .put("provisionerId", infrastructureProvisioner.getUuid())
                        .put("runPlanOnly", runPlanOnly)
                        .put("inheritApprovedPlan", !runPlanOnly)
                        .put("variables", runPlanOnly ? getVars(addEnvVars) : emptyList())
                        .put("environmentVariables", addEnvVars ? getEnvVars() : emptyList())
                        .put("pathToModule", PATH_TO_MODULE)
                        .build())
        .build();
  }

  private PhaseStep addPostDeploymentPhaseStep(InfrastructureProvisioner infrastructureProvisioner, boolean addEnvVars)
      throws IOException {
    PhaseStep phaseStep = addPostDeploymentRunPlanOnlyPhaseStep(infrastructureProvisioner, addEnvVars);

    GraphNode destroy = createDestroyStep(infrastructureProvisioner, false, false);
    phaseStep.getSteps().add(destroy);

    return phaseStep;
  }

  private PhaseStep addPostDeploymentRunPlanOnlyPhaseStep(
      InfrastructureProvisioner infrastructureProvisioner, boolean addEnvVars) throws IOException {
    GraphNode destroyPlan = createDestroyStep(infrastructureProvisioner, true, addEnvVars);
    GraphNode shellScript = createShellScriptStep(true);

    return aPhaseStep(PhaseStepType.POST_DEPLOYMENT, "Post Deployment")
        .addStep(destroyPlan)
        .addStep(shellScript)
        .build();
  }

  private InfrastructureProvisioner createTerragruntProvisioner(boolean useBranch) {
    TerragruntInfrastructureProvisioner terragruntInfrastructureProvisioner =
        TerragruntInfrastructureProvisioner.builder()
            .appId(application.getAppId())
            .name(PROVISIONER_NAME_PREFIX + (useBranch ? EMPTY_STRING : "_WithCommitId"))
            .sourceRepoSettingId(gitConnector.getUuid())
            .sourceRepoBranch(useBranch ? "terragrunt" : null)
            .commitId(useBranch ? null : "c01ad6a8bbb88c6fd1f502975514b1b565353394")
            .skipRefreshBeforeApplyingPlan(true)
            .path("/Terragrunt/")
            .mappingBlueprints(Arrays.asList())
            .secretManagerId(application.getAccountId())
            .build();

    return infrastructureProvisionerGenerator.ensureTerragruntInfrastructureProvisioner(
        seed, owners, terragruntInfrastructureProvisioner);
  }

  private List<Map<String, String>> getVars(boolean addEnvVars) {
    List<Map<String, String>> variables = new ArrayList<>();
    if (!addEnvVars) {
      variables.add(ImmutableMap.of("name", "tfmodule3", "value", "5", "valueType", "TEXT"));
    }
    variables.add(ImmutableMap.of("name", "slmodule3", "value", "5", "valueType", "TEXT"));
    return variables;
  }

  private List<Map<String, String>> getEnvVars() {
    List<Map<String, String>> variables = new ArrayList<>();
    variables.add(ImmutableMap.of("name", "TF_VAR_tfmodule3", "value", "5", "valueType", "TEXT"));
    return variables;
  }

  public static String readFileContent(String filePath, String resourcePath) throws IOException {
    File scriptFile = null;
    scriptFile = new File(resourcePath + PATH_DELIMITER + filePath);

    return FileUtils.readFileToString(scriptFile, "UTF-8");
  }

  private ExecutionArgs getExecutionArgs(Workflow workflow) {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(workflow.getWorkflowType());
    executionArgs.setOrchestrationId(workflow.getUuid());
    return executionArgs;
  }

  private Workflow createAndAssertWorkflow(Workflow workflow) {
    Workflow savedWorkflow =
        WorkflowRestUtils.createWorkflow(bearerToken, application.getAccountId(), application.getUuid(), workflow);
    assertThat(savedWorkflow).isNotNull();
    assertThat(savedWorkflow.getUuid()).isNotEmpty();
    assertThat(savedWorkflow.getWorkflowType()).isEqualTo(ORCHESTRATION);
    return savedWorkflow;
  }
}

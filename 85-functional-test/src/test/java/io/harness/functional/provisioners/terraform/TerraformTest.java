package io.harness.functional.provisioners.terraform;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.beans.WorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.WorkflowUtils;
import io.harness.functional.provisioners.utils.InfraProvisionerUtils;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.EnvironmentGenerator.Environments;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.SecretGenerator;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.ServiceGenerator.Services;
import io.harness.generator.SettingGenerator;
import io.harness.generator.SettingGenerator.Settings;
import io.harness.rule.OwnerRule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.testframework.restutils.InfraProvisionerRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.GraphNode;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.NameValuePair;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.StateType;

import java.util.Arrays;
import java.util.List;

public class TerraformTest extends AbstractFunctionalTest {
  private static final String secretKeyName = "aws_playground_secret_key";
  private static final long TEST_TIMEOUT_IN_MINUTES = 3;

  private final Seed seed = new Seed(0);
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private SettingGenerator settingGenerator;
  @Inject private SecretGenerator secretGenerator;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private ScmSecret scmSecret;
  @Inject private WorkflowUtils workflowUtils;

  private Application application;
  private Service service;
  private Environment environment;
  private TerraformInfrastructureProvisioner terraformInfrastructureProvisioner;
  private Workflow workflow;
  private SettingAttribute settingAttribute;
  private String secretKeyValue;

  private Owners owners;

  @Before
  public void setUp() throws Exception {
    owners = ownerManager.create();
    ensurePredefinedStuff();

    terraformInfrastructureProvisioner = buildProvisionerObject();
    terraformInfrastructureProvisioner = (TerraformInfrastructureProvisioner) InfraProvisionerRestUtils.saveProvisioner(
        application.getAppId(), bearerToken, terraformInfrastructureProvisioner);

    workflow = buildWorkflow(terraformInfrastructureProvisioner);
    workflow =
        WorkflowRestUtils.createWorkflow(bearerToken, application.getAccountId(), application.getUuid(), workflow);
  }

  private void ensurePredefinedStuff() {
    application = applicationGenerator.ensurePredefined(seed, owners, Applications.FUNCTIONAL_TEST);
    service = serviceGenerator.ensurePredefined(seed, owners, Services.FUNCTIONAL_TEST);
    environment = environmentGenerator.ensurePredefined(seed, owners, Environments.FUNCTIONAL_TEST);
    settingAttribute = settingGenerator.ensurePredefined(seed, owners, Settings.TERRAFORM_MAIN_GIT_REPO);
    secretKeyValue = secretGenerator.ensureStored(owners, SecretName.builder().value(secretKeyName).build());
  }

  @Test
  @Owner(emails = VAIBHAV_SI)
  @Category(FunctionalTests.class)
  @Ignore("TODO: need more investigation")
  public void shouldRunTerraformWorkflow() {
    ExecutionArgs executionArgs = prepareExecutionArgs(workflow);
    WorkflowExecution workflowExecution =
        WorkflowRestUtils.startWorkflow(bearerToken, application.getAppId(), environment.getUuid(), executionArgs);
    workflowUtils.checkForWorkflowSuccess(workflowExecution);
  }

  private ExecutionArgs prepareExecutionArgs(Workflow workflow) {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(workflow.getWorkflowType());
    executionArgs.setOrchestrationId(workflow.getUuid());
    return executionArgs;
  }

  private Workflow buildWorkflow(TerraformInfrastructureProvisioner terraformInfrastructureProvisioner)
      throws Exception {
    return aWorkflow()
        .name("Terraform Test")
        .envId(environment.getUuid())
        .workflowType(WorkflowType.ORCHESTRATION)
        .orchestrationWorkflow(
            aCanaryOrchestrationWorkflow()
                .withPreDeploymentSteps(
                    aPhaseStep(PRE_DEPLOYMENT)
                        .addStep(buildTerraformProvisionStep(terraformInfrastructureProvisioner.getUuid()))
                        .build())
                .withPostDeploymentSteps(
                    aPhaseStep(POST_DEPLOYMENT)
                        .addStep(buildTerraformDestroyStep(terraformInfrastructureProvisioner.getUuid()))
                        .build())
                .build())
        .build();
  }

  private GraphNode buildTerraformDestroyStep(String provisionerId) {
    return GraphNode.builder()
        .id(generateUuid())
        .type(StateType.TERRAFORM_DESTROY.getType())
        .name("Terraform Destroy")
        .properties(ImmutableMap.<String, Object>builder().put("provisionerId", provisionerId).build())
        .build();
  }
  private GraphNode buildTerraformProvisionStep(String provisionerId) throws Exception {
    InfrastructureProvisioner provisioner =
        InfraProvisionerRestUtils.getProvisioner(application.getUuid(), bearerToken, provisionerId);
    provisioner = InfraProvisionerUtils.setValuesToProvisioner(provisioner, scmSecret, secretKeyValue);
    return GraphNode.builder()
        .id(generateUuid())
        .type(StateType.TERRAFORM_PROVISION.getType())
        .name("Terraform Provision")
        .properties(ImmutableMap.<String, Object>builder()
                        .put("provisionerId", provisionerId)
                        .put("variables", provisioner.getVariables())
                        .build())
        .build();
  }

  private TerraformInfrastructureProvisioner buildProvisionerObject() {
    List<NameValuePair> variables =
        Arrays.asList(NameValuePair.builder().name("access_key").valueType(Type.TEXT.toString()).build(),
            NameValuePair.builder().name("secret_key").valueType(Type.ENCRYPTED_TEXT.toString()).build());
    return TerraformInfrastructureProvisioner.builder()
        .appId(application.getAppId())
        .name("Terraform Test")
        .sourceRepoSettingId(settingAttribute.getUuid())
        .sourceRepoBranch("master")
        .path("functionalTest/")
        .variables(variables)
        .build();
  }
}
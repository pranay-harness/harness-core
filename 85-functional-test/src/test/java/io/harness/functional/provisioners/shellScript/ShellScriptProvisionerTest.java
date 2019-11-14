package io.harness.functional.provisioners.shellScript;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.YOGESH_CHAUHAN;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.beans.WorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.exception.WingsException;
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
import io.harness.rule.OwnerRule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.testframework.restutils.EnvironmentRestUtils;
import io.harness.testframework.restutils.InfraProvisionerRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.api.DeploymentType;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.BlueprintProperty;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.GraphNode;
import software.wings.beans.InfrastructureMappingBlueprint;
import software.wings.beans.InfrastructureMappingBlueprint.CloudProviderType;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.NameValuePair;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.shellscript.provisioner.ShellScriptInfrastructureProvisioner;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.StateType;

import java.util.Arrays;
import java.util.List;

public class ShellScriptProvisionerTest extends AbstractFunctionalTest {
  private static final String secretKeyName = "aws_playground_secret_key";
  private static final long TEST_TIMEOUT_IN_MINUTES = 3;

  private final Seed seed = new Seed(0);
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private SecretGenerator secretGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private ScmSecret scmSecret;
  @Inject private AccountService accountService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private WorkflowUtils workflowUtils;

  private Application application;
  private Environment environment;
  private ShellScriptInfrastructureProvisioner shellScriptInfrastructureProvisioner;
  private Workflow workflow;
  private String secretKeyValue;
  private Account account;
  private Service service;
  private String infraMappingUuid;

  private Owners owners;

  @Before
  public void setUp() throws Exception {
    owners = ownerManager.create();
    ensurePredefinedStuff();
    account = accountService.get(application.getAccountId());
    shellScriptInfrastructureProvisioner = buildProvisionerObject();
    shellScriptInfrastructureProvisioner =
        (ShellScriptInfrastructureProvisioner) InfraProvisionerRestUtils.saveProvisioner(
            application.getAppId(), bearerToken, shellScriptInfrastructureProvisioner);

    configureInfraMapping();
    workflow = buildWorkflow(shellScriptInfrastructureProvisioner);
    workflow =
        WorkflowRestUtils.createWorkflow(bearerToken, application.getAccountId(), application.getUuid(), workflow);
  }

  private void ensurePredefinedStuff() {
    application = applicationGenerator.ensurePredefined(seed, owners, Applications.FUNCTIONAL_TEST);
    environment = environmentGenerator.ensurePredefined(seed, owners, Environments.FUNCTIONAL_TEST);
    service = serviceGenerator.ensurePredefined(seed, owners, Services.FUNCTIONAL_TEST);
    secretKeyValue = secretGenerator.ensureStored(owners, SecretName.builder().value(secretKeyName).build());
  }

  @Test
  @Owner(developers = YOGESH_CHAUHAN, intermittent = true)
  @Category({FunctionalTests.class})
  public void shouldRunShellScriptWorkflow() {
    ExecutionArgs executionArgs = prepareExecutionArgs(workflow);
    WorkflowExecution workflowExecution =
        WorkflowRestUtils.startWorkflow(bearerToken, application.getAppId(), environment.getUuid(), executionArgs);
    workflowUtils.checkForWorkflowSuccess(workflowExecution);
    checkHosts();
  }

  private void checkHosts() {
    List<Host> hosts = infrastructureMappingService.listHosts(application.getUuid(), infraMappingUuid);
    if (hosts == null) {
      throw new WingsException("Host is null for Infra Mapping");
    }
  }

  private void configureInfraMapping() throws Exception {
    String serviceTemplateId =
        EnvironmentRestUtils.getServiceTemplateId(bearerToken, account, application.getUuid(), environment.getUuid());
    PhysicalInfrastructureMapping physicalInfrastructureMapping =
        aPhysicalInfrastructureMapping()
            .withServiceId(service.getUuid())
            .withInfraMappingType(PHYSICAL_DATA_CENTER_SSH.name())
            .withComputeProviderType("PHYSICAL_DATA_CENTER")
            .withComputeProviderName("Physical Data Center: PHYSICAL_DATA_CENTER")
            .withDeploymentType((DeploymentType.SSH).toString())
            .withComputeProviderSettingId("DEFAULT")
            .withServiceTemplateId(serviceTemplateId)
            .withProvisionerId(shellScriptInfrastructureProvisioner.getUuid())
            .withAutoPopulate(true)
            .build();

    infraMappingUuid = EnvironmentRestUtils.saveInfraMapping(bearerToken, application.getAccountId(),
        application.getUuid(), environment.getUuid(), physicalInfrastructureMapping);
  }

  private Workflow buildWorkflow(ShellScriptInfrastructureProvisioner shellScriptInfrastructureProvisioner)
      throws Exception {
    return aWorkflow()
        .name("Shell Script Provisioner Test")
        .envId(environment.getUuid())
        .workflowType(WorkflowType.ORCHESTRATION)
        .orchestrationWorkflow(aCanaryOrchestrationWorkflow()
                                   .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT)
                                                               .addStep(buildShellScriptProvisionStep(
                                                                   shellScriptInfrastructureProvisioner.getUuid()))
                                                               .build())
                                   .build())
        .build();
  }

  private ExecutionArgs prepareExecutionArgs(Workflow workflow) {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(workflow.getWorkflowType());
    executionArgs.setOrchestrationId(workflow.getUuid());
    return executionArgs;
  }

  private GraphNode buildShellScriptProvisionStep(String provisionerId) throws Exception {
    InfrastructureProvisioner provisioner =
        InfraProvisionerRestUtils.getProvisioner(application.getUuid(), bearerToken, provisionerId);
    provisioner = InfraProvisionerUtils.setValuesToProvisioner(provisioner, scmSecret, secretKeyValue);
    return GraphNode.builder()
        .id(generateUuid())
        .type(StateType.SHELL_SCRIPT_PROVISION.getType())
        .name("Shell Script Provisioner")
        .properties(ImmutableMap.<String, Object>builder()
                        .put("provisionerId", provisionerId)
                        .put("variables", provisioner.getVariables())
                        .build())
        .build();
  }

  private ShellScriptInfrastructureProvisioner buildProvisionerObject() {
    final String scriptBody = "apt-get -y install awscli\n"
        + "aws configure set aws_access_key_id $access_key\n"
        + "aws configure set aws_secret_access_key $secret_key\n"
        + "aws configure set region us-west-1\n"
        + "echo \""
        + "{\n"
        + "  \\\"Instances\\\": [\n"
        + "    {\n"
        + "      \\\"PublicDnsName\\\": \\\"ec2-192-0-2-0.us-west-2.compute.amazonaws.com\\\",\n"
        + "      \\\"Hostname\\\": \\\"ip-192-0-2-0\\\",\n"
        + "      \\\"Ec2InstanceId\\\": \\\"i-5cd23551\\\",\n"
        + "      \\\"SubnetId\\\": \\\"subnet-b8de0ddd\\\"\n"
        + "    }\n"
        + "  ]\n"
        + "}"
        + "\""
        + " > $PROVISIONER_OUTPUT_PATH";

    List<NameValuePair> variables =
        Arrays.asList(NameValuePair.builder().name("access_key").valueType(Type.TEXT.toString()).build(),
            NameValuePair.builder().name("secret_key").valueType(Type.ENCRYPTED_TEXT.toString()).build());
    List<InfrastructureMappingBlueprint> mappingBlueprints =
        Arrays.asList(InfrastructureMappingBlueprint.builder()
                          .serviceId(service.getUuid())
                          .cloudProviderType(CloudProviderType.PHYSICAL_DATA_CENTER)
                          .deploymentType(DeploymentType.SSH)
                          .properties((List<BlueprintProperty>) Arrays.asList(
                              BlueprintProperty.builder()
                                  .name("hostArrayPath")
                                  .value("Instances")
                                  .fields((List<NameValuePair>) Arrays.asList(
                                      NameValuePair.builder().name("Hostname").value("PublicDnsName").build(),
                                      NameValuePair.builder().name("SubnetId").value("SubnetId").build()))
                                  .build()))
                          .build());

    return ShellScriptInfrastructureProvisioner.builder()
        .appId(application.getAppId())
        .name("Shell Script Provisioner Test")
        .scriptBody(scriptBody)
        .variables(variables)
        .mappingBlueprints(mappingBlueprints)
        .build();
  }
}

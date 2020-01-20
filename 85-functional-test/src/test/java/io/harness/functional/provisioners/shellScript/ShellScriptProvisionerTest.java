package io.harness.functional.provisioners.shellScript;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.generator.SettingGenerator.Settings.GITHUB_TEST_CONNECTOR;
import static io.harness.generator.SettingGenerator.Settings.PHYSICAL_DATA_CENTER;
import static io.harness.rule.OwnerRule.ABHINAV;
import static io.harness.rule.OwnerRule.YOGESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.beans.WorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.exception.InvalidRequestException;
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
import io.harness.generator.SettingGenerator;
import io.harness.generator.WorkflowGenerator;
import io.harness.rule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
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
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingBlueprint;
import software.wings.beans.InfrastructureMappingBlueprint.CloudProviderType;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.NameValuePair;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.shellscript.provisioner.ShellScriptInfrastructureProvisioner;
import software.wings.dl.WingsPersistence;
import software.wings.infra.InfrastructureDefinition;
import software.wings.infra.InfrastructureDefinition.InfrastructureDefinitionKeys;
import software.wings.infra.PhysicalInfra;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private SettingGenerator settingGenerator;
  @Inject private InfrastructureDefinitionService infrastructureDefinitionService;
  @Inject private WingsPersistence wingsPersistence;

  private Application application;
  private Environment environment;
  private ShellScriptInfrastructureProvisioner shellScriptInfrastructureProvisioner;
  private Workflow workflow;
  private Workflow workflow_without_provisioner;
  private String secretKeyValue;
  private Account account;
  private Service service;
  private InfrastructureDefinition infrastructureDefinition;
  private Owners owners;

  @Before
  public void setUp() throws Exception {
    owners = ownerManager.create();
    ensurePredefinedStuff();
    account = accountService.get(application.getAccountId());
    resetCache(account.getUuid());
  }

  private void ensurePredefinedStuff() {
    application = applicationGenerator.ensurePredefined(seed, owners, Applications.FUNCTIONAL_TEST);
    assertThat(application).isNotNull();
    service = serviceGenerator.ensurePredefined(seed, owners, Services.FUNCTIONAL_TEST);
    assertThat(service).isNotNull();
    environment = environmentGenerator.ensurePredefined(seed, owners, Environments.FUNCTIONAL_TEST);
    assertThat(environment).isNotNull();
    secretKeyValue = secretGenerator.ensureStored(owners, SecretName.builder().value(secretKeyName).build());
  }

  @Test
  @Owner(developers = YOGESH, intermittent = true)
  @Category({FunctionalTests.class})
  public void shouldRunShellScriptWorkflow() throws Exception {
    shellScriptInfrastructureProvisioner = buildProvisionerObject();
    shellScriptInfrastructureProvisioner =
        (ShellScriptInfrastructureProvisioner) InfraProvisionerRestUtils.saveProvisioner(
            application.getAppId(), bearerToken, shellScriptInfrastructureProvisioner);
    configureInfraDefinition();
    workflow = buildWorkflow(shellScriptInfrastructureProvisioner);
    workflow = workflowGenerator.ensureWorkflow(seed, owners, workflow);
    ExecutionArgs executionArgs = prepareExecutionArgs(workflow);
    resetCache(account.getUuid());
    WorkflowExecution workflowExecution =
        WorkflowRestUtils.startWorkflow(bearerToken, application.getAppId(), environment.getUuid(), executionArgs);
    workflowUtils.checkForWorkflowSuccess(workflowExecution);
    checkHosts();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category({FunctionalTests.class})
  public void runShellScriptWorkflowWithoutProvisioner() {
    workflow_without_provisioner = buildWorkflowWithoutProvisioner();
    workflow_without_provisioner = workflowGenerator.ensureWorkflow(seed, owners, workflow_without_provisioner);
    ExecutionArgs executionArgs = prepareExecutionArgs(workflow_without_provisioner);
    assertThatThrownBy(()
                           -> WorkflowRestUtils.startWorkflow(
                               bearerToken, application.getAppId(), environment.getUuid(), executionArgs))
        .isInstanceOf(WingsException.class);
  }

  private void checkHosts() {
    InfrastructureMapping infrastructureMapping =
        infrastructureDefinitionService.getInfrastructureMapping(service.getUuid(), infrastructureDefinition);
    List<Host> hosts = infrastructureMappingService.listHosts(application.getUuid(), infrastructureMapping.getUuid());
    if (hosts == null) {
      throw new InvalidRequestException("Host is null for Infra Mapping");
    }
  }

  private void configureInfraDefinition() throws Exception {
    final SettingAttribute physicalInfraSettingAttr =
        settingGenerator.ensurePredefined(seed, owners, PHYSICAL_DATA_CENTER);
    final SettingAttribute settingAttribute = settingGenerator.ensurePredefined(seed, owners, GITHUB_TEST_CONNECTOR);
    InfrastructureDefinition physicalInfrastructureDefinition =
        InfrastructureDefinition.builder()
            .name("Shell Script Provisioner Test")
            .infrastructure(PhysicalInfra.builder()
                                .cloudProviderId(physicalInfraSettingAttr.getUuid())
                                .hostConnectionAttrs(settingAttribute.getUuid())
                                .expressions(ImmutableMap.of(
                                    PhysicalInfra.hostname, "Hostname", PhysicalInfra.hostArrayPath, "Instances"))
                                .build()

                    )
            .deploymentType(DeploymentType.SSH)
            .cloudProviderType(software.wings.api.CloudProviderType.PHYSICAL_DATA_CENTER)
            .envId(environment.getUuid())
            .provisionerId(shellScriptInfrastructureProvisioner.getUuid())
            .appId(owners.obtainApplication().getUuid())
            .build();

    infrastructureDefinition = ensureInfrastructureDefinition(physicalInfrastructureDefinition);
  }

  private Workflow buildWorkflowWithoutProvisioner() {
    return aWorkflow()
        .name("Shell Script Provisioner without Provisioner Test")
        .envId(environment.getUuid())
        .workflowType(WorkflowType.ORCHESTRATION)
        .orchestrationWorkflow(
            aCanaryOrchestrationWorkflow()
                .withPreDeploymentSteps(
                    aPhaseStep(PRE_DEPLOYMENT).addStep(buildShellScriptProvisionStepWithoutProvisioner()).build())
                .build())
        .build();
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
    List<Map<String, String>> variables = new ArrayList<>();
    provisioner.getVariables().forEach(var -> variables.add(new HashMap<String, String>() {
      {
        put("name", var.getName());
        put("value", var.getValue());
        put("valueType", var.getValueType());
      }
    }));
    return GraphNode.builder()
        .id(generateUuid())
        .type(StateType.SHELL_SCRIPT_PROVISION.getType())
        .name("Shell Script Provisioner")
        .properties(ImmutableMap.<String, Object>builder()
                        .put("provisionerId", provisionerId)
                        .put("variables", variables)
                        .build())
        .build();
  }

  private GraphNode buildShellScriptProvisionStepWithoutProvisioner() {
    return GraphNode.builder()
        .id(generateUuid())
        .type(StateType.SHELL_SCRIPT_PROVISION.getType())
        .name("Shell Script Provisioner")
        .build();
  }

  private ShellScriptInfrastructureProvisioner buildProvisionerObject() {
    final String scriptBody = "echo \""
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

  private InfrastructureDefinition ensureInfrastructureDefinition(InfrastructureDefinition infrastructureDefinition) {
    InfrastructureDefinition existing = exists(infrastructureDefinition);
    if (existing != null) {
      return existing;
    }
    return infrastructureDefinitionService.save(infrastructureDefinition, false, true);
  }

  private InfrastructureDefinition exists(InfrastructureDefinition infrastructureDefinition) {
    return wingsPersistence.createQuery(InfrastructureDefinition.class)
        .filter(InfrastructureDefinitionKeys.appId, infrastructureDefinition.getAppId())
        .filter(InfrastructureDefinitionKeys.envId, infrastructureDefinition.getEnvId())
        .filter(InfrastructureDefinitionKeys.name, infrastructureDefinition.getName())
        .get();
  }
}

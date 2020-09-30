package io.harness.functional.connectors;

import static io.harness.generator.InfrastructureDefinitionGenerator.InfrastructureDefinitions.GCP_HELM;
import static io.harness.generator.InfrastructureDefinitionGenerator.InfrastructureDefinitions.K8S_ROLLING_TEST;
import static io.harness.generator.SettingGenerator.Settings.ACCOUNT_LEVEL_GIT_CONNECTOR;
import static io.harness.rule.OwnerRule.ABOSII;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.InfrastructureType.PCF_INFRASTRUCTURE;
import static software.wings.beans.Service.builder;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.WorkflowType;
import io.harness.category.element.CDFunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.utils.K8SUtils;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.EnvironmentGenerator.Environments;
import io.harness.generator.InfrastructureDefinitionGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.SettingGenerator;
import io.harness.generator.WorkflowGenerator;
import io.harness.generator.artifactstream.ArtifactStreamManager;
import io.harness.k8s.model.HelmVersion;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.ArtifactStreamRestUtils;
import io.harness.testframework.restutils.ServiceVariablesUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.BasicOrchestrationWorkflow;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.FeatureName;
import software.wings.beans.GitFileConfig;
import software.wings.beans.GraphNode;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.WorkflowService;
import software.wings.utils.ArtifactType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

public class AccountLevelGitConnectorFunctionalTest extends AbstractFunctionalTest {
  private static final long TIMEOUT = 1200000; // 20 minutes
  private static final String RESOURCE_NAME_FORMAT = "git-account-level-%s-%s";
  private static final String PCF_FUNCTIONAL_TEST_REPO_NAME = "pcf-functional-test";
  private static final String K8S_FUNCTIONAL_TEST_REPO_NAME = "k8s-functional-test";
  private static final String REPO_NAME_SERVICE_VARIABLE = "repoName";

  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private SettingGenerator settingGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private InfrastructureDefinitionGenerator infrastructureDefinitionGenerator;
  @Inject private ApplicationManifestService applicationManifestService;
  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private WorkflowService workflowService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private ArtifactStreamManager artifactStreamManager;

  private final Randomizer.Seed seed = new Randomizer.Seed(0);
  private OwnerManager.Owners owners;
  private Application application;
  private SettingAttribute accountGitConnector;

  @Before
  public void setUp() {
    owners = ownerManager.create();
    application = owners.obtainApplication(
        () -> applicationGenerator.ensurePredefined(seed, owners, ApplicationGenerator.Applications.GENERIC_TEST));
    assertThat(application).isNotNull();
    if (!featureFlagService.isEnabled(FeatureName.GIT_ACCOUNT_SUPPORT, application.getAccountId())) {
      featureFlagService.enableAccount(FeatureName.GIT_ACCOUNT_SUPPORT, application.getAccountId());
    }

    accountGitConnector = settingGenerator.ensurePredefined(seed, owners, ACCOUNT_LEVEL_GIT_CONNECTOR);
  }

  @Test(timeout = TIMEOUT)
  @Owner(developers = ABOSII)
  @Category(CDFunctionalTests.class)
  public void testPcfUsingAccountLevelGitConnector() {
    Service service = createPCFService(getName("pcf", "service"));
    updateApplicationManifest(createServiceManifest(service, "pcf-app1", StoreType.Remote));
    setRepoNameServiceVariable(service, PCF_FUNCTIONAL_TEST_REPO_NAME);

    Environment environment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);
    InfrastructureDefinition infraDefinition =
        infrastructureDefinitionGenerator.ensurePredefined(seed, owners, PCF_INFRASTRUCTURE, bearerToken);

    Workflow workflow = createBasicWorkflow(getName("pcf", "wf"), service, infraDefinition);
    workflow = workflowGenerator.ensureWorkflow(seed, owners, workflow);
    setupPcfWorkflow(workflow);

    resetCache(getAccount().getUuid());
    WorkflowExecution workflowExecution = runWorkflow(
        bearerToken, application.getUuid(), environment.getUuid(), getExecutionArgs(workflow, environment, service));
    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test(timeout = TIMEOUT)
  @Owner(developers = ABOSII)
  @Category(CDFunctionalTests.class)
  public void testK8sUsingAccountLevelGitConnector() {
    Service service = createK8sService(getName("k8s", "service"));
    updateApplicationManifest(createServiceManifest(service, "manifests/basic", StoreType.Remote));
    setRepoNameServiceVariable(service, K8S_FUNCTIONAL_TEST_REPO_NAME);

    Environment environment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);
    InfrastructureDefinition infraDefinition =
        infrastructureDefinitionGenerator.ensurePredefined(seed, owners, K8S_ROLLING_TEST);

    Workflow workflow = K8SUtils.createWorkflow(application.getUuid(), environment.getUuid(), service.getUuid(),
        infraDefinition.getUuid(), getName("k8s", "wf"), OrchestrationWorkflowType.ROLLING, bearerToken,
        getAccount().getUuid());

    WorkflowExecution workflowExecution = runWorkflow(
        bearerToken, application.getUuid(), environment.getUuid(), getExecutionArgs(workflow, environment, service));
    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test(timeout = TIMEOUT)
  @Owner(developers = ABOSII)
  @Category(CDFunctionalTests.class)
  @Ignore("There is no clear reason why this test is failing on jenkins. Will investigate it later")
  public void testHelmUsingAccountLevelGitConnector() {
    Service service = createHelmService("helm-account-level-git");
    updateApplicationManifest(createServiceManifest(service, "charts/basic", StoreType.HelmSourceRepo));
    setRepoNameServiceVariable(service, K8S_FUNCTIONAL_TEST_REPO_NAME);

    Environment environment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);
    InfrastructureDefinition infraDefinition =
        infrastructureDefinitionGenerator.ensurePredefined(seed, owners, GCP_HELM);

    Workflow workflow = createBasicWorkflow(getName("helm", "wf"), service, infraDefinition);
    workflow = workflowGenerator.ensureWorkflow(seed, owners, workflow);
    setupHelmWorkflow(workflow);

    WorkflowExecution workflowExecution = runWorkflow(
        bearerToken, application.getUuid(), environment.getUuid(), getExecutionArgs(workflow, environment, service));
    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  private void setRepoNameServiceVariable(Service service, String value) {
    ServiceVariable repoNameVariable = ServiceVariable.builder()
                                           .accountId(getAccount().getUuid())
                                           .value(value.toCharArray())
                                           .name(REPO_NAME_SERVICE_VARIABLE)
                                           .type(Type.TEXT)
                                           .entityType(EntityType.SERVICE)
                                           .entityId(service.getUuid())
                                           .build();
    repoNameVariable.setAppId(service.getAppId());
    ServiceVariablesUtils.addOrGetServiceVariable(bearerToken, repoNameVariable);
  }

  private Service createPCFService(String name) {
    owners.add(serviceGenerator.ensureService(seed, owners,
        builder().name(name).artifactType(ArtifactType.PCF).deploymentType(DeploymentType.PCF).isPcfV2(true).build()));
    ArtifactStream artifactStream = artifactStreamManager.ensurePredefined(
        seed, owners, ArtifactStreamManager.ArtifactStreams.HARNESS_SAMPLE_DOCKER);
    Service service = owners.obtainService();
    service.setArtifactStreamIds(new ArrayList<>(Arrays.asList(artifactStream.getUuid())));
    return service;
  }

  private Service createK8sService(String name) {
    owners.add(serviceGenerator.ensureService(seed, owners,
        builder()
            .name(name)
            .artifactType(ArtifactType.DOCKER)
            .deploymentType(DeploymentType.KUBERNETES)
            .isK8sV2(true)
            .build()));
    ArtifactStream artifactStream = artifactStreamManager.ensurePredefined(
        seed, owners, ArtifactStreamManager.ArtifactStreams.HARNESS_SAMPLE_DOCKER);
    Service service = owners.obtainService();
    service.setArtifactStreamIds(new ArrayList<>(Arrays.asList(artifactStream.getUuid())));
    return service;
  }

  private Service createHelmService(String name) {
    owners.add(serviceGenerator.ensureService(seed, owners,
        builder()
            .name(name)
            .artifactType(ArtifactType.DOCKER)
            .helmVersion(HelmVersion.V3)
            .deploymentType(DeploymentType.HELM)
            .appId(application.getUuid())
            .build()));

    return owners.obtainService();
  }

  private ApplicationManifest createServiceManifest(Service service, String filePath, StoreType storeType) {
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder()
            .serviceId(service.getUuid())
            .kind(AppManifestKind.K8S_MANIFEST)
            .accountId(getAccount().getUuid())
            .storeType(storeType)
            .gitFileConfig(GitFileConfig.builder()
                               .connectorId(accountGitConnector.getUuid())
                               .branch("master")
                               .useBranch(true)
                               .filePath(filePath)
                               .repoName(format("${serviceVariable.%s}", REPO_NAME_SERVICE_VARIABLE))
                               .build())
            .build();
    applicationManifest.setAppId(service.getAppId());
    return applicationManifest;
  }

  private void updateApplicationManifest(ApplicationManifest applicationManifest) {
    final ApplicationManifest manifestByServiceId = applicationManifestService.getManifestByServiceId(
        applicationManifest.getAppId(), applicationManifest.getServiceId());
    if (manifestByServiceId != null) {
      applicationManifest.setUuid(manifestByServiceId.getUuid());
      applicationManifestService.update(applicationManifest);
    } else {
      applicationManifestService.create(applicationManifest);
    }
  }

  private Workflow createBasicWorkflow(String name, Service service, InfrastructureDefinition infraDefinition) {
    Workflow workflow = aWorkflow()
                            .name(name)
                            .appId(service.getAppId())
                            .envId(infraDefinition.getEnvId())
                            .infraDefinitionId(infraDefinition.getUuid())
                            .serviceId(service.getUuid())
                            .workflowType(WorkflowType.ORCHESTRATION)
                            .orchestrationWorkflow(aBasicOrchestrationWorkflow().build())
                            .build();
    return workflow;
  }

  private ExecutionArgs getExecutionArgs(Workflow workflow, Environment environment, Service service) {
    String artifactId = ArtifactStreamRestUtils.getArtifactStreamId(
        bearerToken, application.getUuid(), environment.getUuid(), service.getUuid());
    Artifact artifact = new Artifact();
    artifact.setUuid(artifactId);

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(workflow.getWorkflowType());
    executionArgs.setOrchestrationId(workflow.getUuid());
    executionArgs.setArtifacts(Collections.singletonList(artifact));

    return executionArgs;
  }

  private void setupHelmWorkflow(Workflow workflow) {
    BasicOrchestrationWorkflow orchestrationWorkflow = (BasicOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    WorkflowPhase workflowPhase = orchestrationWorkflow.getWorkflowPhases().get(0);
    GraphNode helmDeployStep = workflowPhase.getPhaseSteps().get(0).getSteps().get(0);
    Map<String, Object> properties = helmDeployStep.getProperties();
    properties.put("helmReleaseNamePrefix", "${service.name}-${infra.helm.shortId}");
    helmDeployStep.setProperties(properties);
    // Starting with v3, helm doesn't manage anymore namespace. This script will create namespace if it's missing
    // (create-namespace alternative is available starting with 3.2)
    GraphNode createNamespaceStep =
        GraphNode.builder()
            .name("Create namespace")
            .type("SHELL_SCRIPT")
            .properties(ImmutableMap.of("scriptType", "BASH", "scriptString",
                "export KUBECONFIG=${HARNESS_KUBE_CONFIG_PATH}\nkubectl get namespace ${infra.kubernetes.namespace} || kubectl create namespace ${infra.kubernetes.namespace}",
                "executeOnDelegate", true, "timeoutMillis", 60000))
            .build();
    if (workflowPhase.getPhaseSteps().get(0).getSteps().size() > 1) {
      workflowPhase.getPhaseSteps().get(0).getSteps().set(0, createNamespaceStep);
    } else {
      workflowPhase.getPhaseSteps().get(0).getSteps().add(0, createNamespaceStep);
    }

    workflowService.updateWorkflow(workflow, false);
  }

  private void setupPcfWorkflow(Workflow workflow) {
    BasicOrchestrationWorkflow orchestrationWorkflow = (BasicOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    WorkflowPhase workflowPhase = orchestrationWorkflow.getWorkflowPhases().get(0);
    GraphNode pcfAppResize = workflowPhase.getPhaseSteps().get(1).getSteps().get(0);
    Map<String, Object> properties = pcfAppResize.getProperties();
    properties.put("instanceCount", "1");

    workflowService.updateWorkflow(workflow, false);
  }

  private String getName(String resource, String type) {
    return format(RESOURCE_NAME_FORMAT, resource, type);
  }
}

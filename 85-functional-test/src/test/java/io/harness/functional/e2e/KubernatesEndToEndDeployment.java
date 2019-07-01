package io.harness.functional.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;

import io.harness.beans.WorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.testframework.framework.GlobalSettingsDataStorage;
import io.harness.testframework.restutils.ApplicationRestUtils;
import io.harness.testframework.restutils.ArtifactStreamRestUtils;
import io.harness.testframework.restutils.EnvironmentRestUtils;
import io.harness.testframework.restutils.ExecutionRestUtils;
import io.harness.testframework.restutils.ServiceRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;
import io.restassured.path.json.JsonPath;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.utils.ArtifactType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class KubernatesEndToEndDeployment extends AbstractFunctionalTest {
  ExecutionRestUtils executionRestUtil = new ExecutionRestUtils();
  static Map<String, String> availableGlobalDataMap = null;
  static GcpKubernetesInfrastructureMapping gcpInfra = null;
  private static Application application;
  private static Environment environment;
  private static Workflow workflow;
  private static WorkflowExecution workflowExecution;
  private static String gcpInfraId;
  private static String dockerRegistryId;
  private static String cloudProviderId;
  private static String serviceId;

  @Test
  @Category(FunctionalTests.class)
  public void t1_testApplicationCreation() {
    availableGlobalDataMap = GlobalSettingsDataStorage.getAvailableGlobalDataMap(bearerToken, getAccount());
    application = anApplication().name("Sample App" + System.currentTimeMillis()).build();
    application = ApplicationRestUtils.createApplication(bearerToken, getAccount(), application);
    assertThat(application).isNotNull();
  }

  @Test
  @Category(FunctionalTests.class)
  public void t2_testServiceCreation() {
    Service service = Service.builder().name("K8sService").artifactType(ArtifactType.DOCKER).build();
    serviceId = ServiceRestUtils.createService(bearerToken, getAccount().getUuid(), application.getAppId(), service);
    DockerArtifactStream dockerArtifactStream = DockerArtifactStream.builder()
                                                    .appId(application.getUuid())
                                                    .serviceId(serviceId)
                                                    .settingId(availableGlobalDataMap.get("Harness Docker Hub"))
                                                    .imageName("library/nginx")
                                                    .autoPopulate(true)
                                                    .build();
    JsonPath response = ArtifactStreamRestUtils.configureDockerArtifactStream(
        bearerToken, getAccount().getUuid(), application.getAppId(), dockerArtifactStream);
    assertThat(response).isNotNull();
  }

  @Test
  @Category(FunctionalTests.class)
  public void t3_testEnvironmentCreation() {
    Environment myEnv = anEnvironment().name("MyEnv").environmentType(EnvironmentType.PROD).build();
    environment = EnvironmentRestUtils.createEnvironment(bearerToken, getAccount(), application.getAppId(), myEnv);
    assertThat(environment).isNotNull();

    String serviceTemplateId = EnvironmentRestUtils.getServiceTemplateId(
        bearerToken, getAccount(), application.getUuid(), environment.getUuid());

    GcpKubernetesInfrastructureMapping gcpInfraMapping =
        aGcpKubernetesInfrastructureMapping()
            .withClusterName("us-west1-a/qa-target")
            .withNamespace("default")
            .withServiceId(serviceId)
            .withDeploymentType(DeploymentType.KUBERNETES.name())
            .withComputeProviderSettingId(availableGlobalDataMap.get("harness-exploration"))
            .withServiceTemplateId(serviceTemplateId)
            .withComputeProviderType("GCP")
            .withComputeProviderName("Google Cloud Platform: harness-exploration")
            .withInfraMappingType(InfrastructureMappingType.GCP_KUBERNETES.getName())
            .withAutoPopulate(true)
            .build();

    gcpInfra = EnvironmentRestUtils.configureInfraMapping(
        bearerToken, getAccount(), application.getUuid(), environment.getUuid(), gcpInfraMapping);

    assertThat(gcpInfra).isNotNull();
  }

  @Test
  @Category(FunctionalTests.class)
  public void t4_testWorkflowCreation() throws Exception {
    workflow = aWorkflow()
                   .name("SampleWF")
                   .envId(environment.getUuid())
                   .serviceId(serviceId)
                   .infraMappingId(gcpInfra.getUuid())
                   .workflowType(WorkflowType.ORCHESTRATION)
                   .orchestrationWorkflow(aBasicOrchestrationWorkflow()
                                              .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                                              .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                                              .build())
                   .build();
    workflow = WorkflowRestUtils.createWorkflow(bearerToken, getAccount().getUuid(), application.getUuid(), workflow);
    assertThat(workflow).isNotNull();
  }

  @Test
  @Category(FunctionalTests.class)
  public void t5_testDeployWorkflow() {
    String artifactId = ArtifactStreamRestUtils.getArtifactStreamId(
        bearerToken, application.getAppId(), environment.getUuid(), serviceId);
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(workflow.getWorkflowType());
    List<Artifact> artifacts = new ArrayList<>();
    Artifact artifact = new Artifact();
    artifact.setUuid(artifactId);
    artifacts.add(artifact);
    executionArgs.setArtifacts(artifacts);
    executionArgs.setOrchestrationId(workflow.getUuid());

    WorkflowExecution workflowExecution =
        executionRestUtil.runWorkflow(bearerToken, application.getAppId(), environment.getUuid(), executionArgs);
    assertThat(workflowExecution).isNotNull();

    String status = ExecutionRestUtils.getWorkflowExecutionStatus(
        bearerToken, getAccount(), application.getAppId(), workflowExecution.getUuid());
    if (!(status.equals("RUNNING") || status.equals("QUEUED"))) {
      Assert.fail("ERROR: Execution did not START");
    }
  }
}
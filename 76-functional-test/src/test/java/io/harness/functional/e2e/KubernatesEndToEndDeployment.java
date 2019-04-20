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

import io.harness.RestUtils.ApplicationRestUtils;
import io.harness.RestUtils.ArtifactStreamRestUtils;
import io.harness.RestUtils.EnvironmentRestUtils;
import io.harness.RestUtils.ExecutionRestUtils;
import io.harness.RestUtils.ServiceRestUtils;
import io.harness.RestUtils.WorkflowRestUtils;
import io.harness.beans.WorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.helper.GlobalSettingsDataStorage;
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
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.utils.ArtifactType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class KubernatesEndToEndDeployment extends AbstractFunctionalTest {
  ApplicationRestUtils applicationRestUtil = new ApplicationRestUtils();
  ServiceRestUtils serviceRestUtil = new ServiceRestUtils();
  EnvironmentRestUtils environmentRestUtil = new EnvironmentRestUtils();
  WorkflowRestUtils workflowRestUtil = new WorkflowRestUtils();
  ArtifactStreamRestUtils artifactStreamRestUtil = new ArtifactStreamRestUtils();
  ExecutionRestUtils executionRestUtil = new ExecutionRestUtils();
  GlobalSettingsDataStorage globalSettingsDataStorage = new GlobalSettingsDataStorage();
  static Map<String, String> availableGlobalDataMap = null;
  static Application sampleApp = null;
  static Service sampleService = null;
  static Environment sampleEnvironment = null;
  static GcpKubernetesInfrastructureMapping gcpInfra = null;
  static Workflow sampleWF = null;

  @Test
  @Category(FunctionalTests.class)
  public void t1_testApplicationCreation() {
    availableGlobalDataMap = globalSettingsDataStorage.getAvailableGlobalDataMap();
    Application application = anApplication().withName("Sample App" + System.currentTimeMillis()).build();
    sampleApp = applicationRestUtil.createApplication(application);
    assertThat(sampleApp).isNotNull();
  }

  @Test
  @Category(FunctionalTests.class)
  public void t2_testServiceCreation() {
    Service service = Service.builder().name("SampleService").artifactType(ArtifactType.DOCKER).build();
    sampleService = serviceRestUtil.createService(sampleApp.getAppId(), service);
    DockerArtifactStream dockerArtifactStream = DockerArtifactStream.builder()
                                                    .appId(sampleApp.getUuid())
                                                    .serviceId(sampleService.getUuid())
                                                    .settingId(availableGlobalDataMap.get("Harness Docker Hub"))
                                                    .imageName("library/nginx")
                                                    .autoPopulate(true)
                                                    .build();
    ArtifactStream artifactStream =
        artifactStreamRestUtil.configureDockerArtifactStream(sampleApp.getAppId(), dockerArtifactStream);
    assertThat(artifactStream).isNotNull();
  }

  @Test
  @Category(FunctionalTests.class)
  public void t3_testEnvironmentCreation() {
    Environment myEnv = anEnvironment().withName("MyEnv").withEnvironmentType(EnvironmentType.PROD).build();
    sampleEnvironment = environmentRestUtil.createEnvironment(sampleApp.getAppId(), myEnv);
    assertThat(sampleEnvironment).isNotNull();

    String serviceTemplateId =
        environmentRestUtil.getServiceTemplateId(sampleApp.getUuid(), sampleEnvironment.getUuid());

    GcpKubernetesInfrastructureMapping gcpInfraMapping =
        aGcpKubernetesInfrastructureMapping()
            .withClusterName("us-west1-a/qa-target")
            .withNamespace("default")
            .withServiceId(sampleService.getUuid())
            .withDeploymentType(DeploymentType.KUBERNETES.name())
            .withComputeProviderSettingId(availableGlobalDataMap.get("harness-exploration"))
            .withServiceTemplateId(serviceTemplateId)
            .withComputeProviderType("GCP")
            .withComputeProviderName("Google Cloud Platform: harness-exploration")
            .withInfraMappingType(InfrastructureMappingType.GCP_KUBERNETES.getName())
            .withAutoPopulate(true)
            .build();

    gcpInfra =
        environmentRestUtil.configureInfraMapping(sampleApp.getUuid(), sampleEnvironment.getUuid(), gcpInfraMapping);

    assertThat(gcpInfra).isNotNull();
  }

  @Test
  @Category(FunctionalTests.class)
  public void t4_testWorkflowCreation() throws Exception {
    Workflow workflow = aWorkflow()
                            .name("SampleWF")
                            .envId(sampleEnvironment.getUuid())
                            .serviceId(sampleService.getUuid())
                            .infraMappingId(gcpInfra.getUuid())
                            .workflowType(WorkflowType.ORCHESTRATION)
                            .orchestrationWorkflow(aBasicOrchestrationWorkflow()
                                                       .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                                                       .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                                                       .build())
                            .build();

    sampleWF = workflowRestUtil.createWorkflow(getAccount().getUuid(), sampleApp.getUuid(), workflow);

    assertThat(sampleWF).isNotNull();
  }

  @Test
  @Category(FunctionalTests.class)
  public void t5_testDeployWorkflow() {
    String artifactId = artifactStreamRestUtil.getArtifactStreamId(
        sampleApp.getAppId(), sampleEnvironment.getUuid(), sampleService.getUuid());
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(sampleWF.getWorkflowType());
    List<Artifact> artifacts = new ArrayList<>();
    Artifact artifact = new Artifact();
    artifact.setUuid(artifactId);
    artifacts.add(artifact);
    executionArgs.setArtifacts(artifacts);
    executionArgs.setOrchestrationId(sampleWF.getUuid());

    WorkflowExecution workflowExecution =
        executionRestUtil.runWorkflow(sampleApp.getAppId(), sampleEnvironment.getUuid(), executionArgs);
    assertThat(workflowExecution).isNotNull();

    String status = executionRestUtil.getExecutionStatus(sampleApp.getAppId(), workflowExecution.getUuid());
    assertThat(status).isEqualTo("SUCCESS");
  }
}
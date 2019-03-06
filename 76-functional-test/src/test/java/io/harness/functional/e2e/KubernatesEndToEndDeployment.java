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

import io.harness.RestUtils.ApplicationRestUtil;
import io.harness.RestUtils.ArtifactStreamRestUtil;
import io.harness.RestUtils.EnvironmentRestUtil;
import io.harness.RestUtils.ExecutionRestUtil;
import io.harness.RestUtils.ServiceRestUtil;
import io.harness.RestUtils.WorkflowRestUtil;
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
import software.wings.beans.ExecutionCredential.ExecutionType;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.SSHExecutionCredential;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.common.Constants;
import software.wings.utils.ArtifactType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class KubernatesEndToEndDeployment extends AbstractFunctionalTest {
  ApplicationRestUtil applicationRestUtil = new ApplicationRestUtil();
  ServiceRestUtil serviceRestUtil = new ServiceRestUtil();
  EnvironmentRestUtil environmentRestUtil = new EnvironmentRestUtil();
  WorkflowRestUtil workflowRestUtil = new WorkflowRestUtil();
  ArtifactStreamRestUtil artifactStreamRestUtil = new ArtifactStreamRestUtil();
  ExecutionRestUtil executionRestUtil = new ExecutionRestUtil();
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
    Workflow workflow =
        aWorkflow()
            .withName("SampleWF")
            .withEnvId(sampleEnvironment.getUuid())
            .withServiceId(sampleService.getUuid())
            .withInfraMappingId(gcpInfra.getUuid())
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withOrchestrationWorkflow(
                aBasicOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build();

    sampleWF = workflowRestUtil.createWorkflow(getAccount().getUuid(), sampleApp.getUuid(), workflow);

    assertThat(sampleWF).isNotNull();
  }

  @Test
  @Category(FunctionalTests.class)
  public void t5_testDeployWorkflow() {
    String artifactStreamId = artifactStreamRestUtil.getArtifactStreamId(
        sampleApp.getAppId(), sampleEnvironment.getUuid(), sampleService.getUuid());
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(sampleWF.getWorkflowType());
    List<Artifact> artifacts = new ArrayList<>();
    Artifact artifact = new Artifact();
    artifact.setUuid(artifactStreamId);
    artifacts.add(artifact);
    executionArgs.setArtifacts(artifacts);
    executionArgs.setExecutionCredential(
        SSHExecutionCredential.Builder.aSSHExecutionCredential().withExecutionType(ExecutionType.SSH).build());
    executionArgs.setOrchestrationId(sampleWF.getUuid());

    WorkflowExecution workflowExecution =
        executionRestUtil.runWorkflow(sampleApp.getAppId(), sampleEnvironment.getUuid(), executionArgs);
    assertThat(workflowExecution).isNotNull();

    String status = executionRestUtil.getExecutionStatus(sampleApp.getAppId(), workflowExecution.getUuid());
    assertThat(status).isEqualTo("SUCCESS");
  }
}
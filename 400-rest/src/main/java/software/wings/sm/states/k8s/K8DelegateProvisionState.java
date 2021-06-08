package software.wings.sm.states.k8s;

import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.FeatureName;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SweepingOutputInstance;
import io.harness.exception.WingsException;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.KubernetesHelperService;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;

import software.wings.beans.DockerConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.delegatetasks.ondemand.OnDemandDelegateService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.verification.CVActivityLogService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.states.mixin.SweepingOutputStateMixin;

import com.github.reinert.jjschema.Attributes;
import com.google.inject.Inject;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.mongodb.morphia.annotations.Transient;

@FieldNameConstants(innerTypeName = "K8DelegateSpawnStateKeys")
@Attributes
@Slf4j
public class K8DelegateProvisionState extends State implements SweepingOutputStateMixin {
  private static final String kubectlBaseDir = "./client-tools/kubectl/";
  private static final String defaultKubectlVersion = "v1.13.2";
  private static String kubectlPath = kubectlBaseDir + defaultKubectlVersion;
  @Getter @Setter private String kubernetesConnectorId;
  @Getter @Setter private String delegateName;
  @Getter @Setter private String cpu;
  @Getter @Setter private String memory;
  @Getter @Setter private String artifactVersion;
  @Getter @Setter private SweepingOutputInstance.Scope sweepingOutputScope;
  @Getter @Setter private String sweepingOutputName;
  @Transient @Inject @Getter KryoSerializer kryoSerializer;
  @Inject private KubernetesHelperService kubernetesHelperService;
  @Inject private SettingsService settingsService;
  @Inject private EncryptionService encryptionService;
  @Inject protected SecretManager secretManager;
  @Inject private KubernetesContainerService kubernetesContainerService;
  @Inject private DelegateService delegateService;
  @Inject private CVActivityLogService cvActivityLogService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private ArtifactService artifactService;
  @Inject private BuildSourceService buildSourceService;

  @Inject private FeatureFlagService featureFlagService;

  @Inject private OnDemandDelegateService onDemandDelegateService;

  public K8DelegateProvisionState(String name) {
    super(name, StateType.DELEGATE_PROVISION.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      String artifactVersion = context.renderExpression(this.artifactVersion);
      Artifact artifact = (Artifact) ((ExecutionContextImpl) context).getContextMap().get("artifact");
      String serviceId = artifact.getServiceIds().get(0);
      PageResponse<Artifact> artifacts =
          artifactService.listArtifactsForService(context.getAppId(), serviceId, new PageRequest<>());
      boolean artifactExists = artifacts.stream().anyMatch(
          existingArtifact -> existingArtifact.getUiDisplayName().equals("Tag# " + artifactVersion));
      if (artifactExists) {
        cvActivityLogService.getLoggerByStateExecutionId(context.getAccountId(), context.getStateExecutionInstanceId())
            .info("Artifact already exists. Continuing the deployment");
      } else {
        cvActivityLogService.getLoggerByStateExecutionId(context.getAccountId(), context.getStateExecutionInstanceId())
            .info("Artifact not collected yet. Continuing to download the artifact");
        SettingAttribute settingAttribute = settingsService.get(artifact.getSettingId());
        DockerConfig dockerConfig = (DockerConfig) settingAttribute.getValue();
        buildSourceService.collectArtifact(context.getAppId(), artifact.getArtifactStreamId(),
            aBuildDetails()
                .withNumber(artifactVersion)
                .withBuildUrl(dockerConfig.getDockerRegistryUrl() + "/" + artifact.getArtifactSourceName() + "/tags/"
                    + artifactVersion)
                .withUiDisplayName("Tag# " + artifactVersion)
                .withMetadata(artifact.getMetadata())
                .build());
        cvActivityLogService.getLoggerByStateExecutionId(context.getAccountId(), context.getStateExecutionInstanceId())
            .info("Artifact " + artifactVersion + " collected. Continuing to deployment");
      }
      log.info("Installing kubectl");
      cvActivityLogService.getLoggerByStateExecutionId(context.getAccountId(), context.getStateExecutionInstanceId())
          .info("Installing kubectl on harness manager");
      OnDemandDelegateHelper.installKubectl();
      log.info("Installed kubectl");
      cvActivityLogService.getLoggerByStateExecutionId(context.getAccountId(), context.getStateExecutionInstanceId())
          .info("kubectl installation complete");
      SettingAttribute settingAttribute = settingsService.get(kubernetesConnectorId);
      KubernetesClusterConfig kubernetesClusterConfig = (KubernetesClusterConfig) settingAttribute.getValue();
      List<EncryptedDataDetail> encryptedDataDetails = secretManager.getEncryptionDetails(
          kubernetesClusterConfig, context.getAppId(), context.getWorkflowExecutionId());
      encryptionService.decrypt(kubernetesClusterConfig, encryptedDataDetails, false);
      KubernetesConfig kubernetesConfig = kubernetesClusterConfig.createKubernetesConfig("harness-delegate");

      String kubeConfigFile = kubernetesContainerService.getConfigFileContent(kubernetesConfig);
      File kubeConf = File.createTempFile(context.getAccountId() + "kubeconfig", null);
      try (BufferedWriter bw = new BufferedWriter(new FileWriter(kubeConf))) {
        bw.write(kubeConfigFile);
      }
      log.info("Location of kubeConfig file is " + kubeConf.getAbsolutePath());

      final String nameOfDelegate = delegateName == null ? "ondemand-delegate" : delegateName;
      // TODO: Fix the manager URL somehow.
      File yamlFile = delegateService.getYamlForKubernetesDelegate("https://pr.harness.io/hackathon-ondemand",
          "https://pr.harness.io/hackathon-ondemand", context.getAccountId(), nameOfDelegate, null, null);

      if (featureFlagService.isFeatureFlagEnabled(FeatureName.ONDEMAND_SIDECAR.name(), context.getAccountId())) {
        onDemandDelegateService.enqueue(
            context.getAccountId(), kubeConfigFile, FileUtils.readFileToString(yamlFile, "UTF-8"));
      } else {
        Kubectl kubectl = Kubectl.client(kubectlPath + "/kubectl", kubeConf.getAbsolutePath());
        String finalCmd = kubectl.apply().filename(yamlFile.getAbsolutePath()).command();
        log.info("final command to be executed is " + finalCmd);
        boolean response = OnDemandDelegateHelper.executeCommand(finalCmd, 10);
        if (!response) {
          log.error("Issue while running command for kubectl");
          return ExecutionResponse.builder()
              .async(false)
              .errorMessage("Error while starting up the delegate with kubectl command")
              .executionStatus(ExecutionStatus.ERROR)
              .build();
        }
        cvActivityLogService.getLoggerByStateExecutionId(context.getAccountId(), context.getStateExecutionInstanceId())
            .info("Delegate start command successful");
      }

      // wait until delegate is up
      while (!delegateService.checkDelegateConnectedByName(context.getAccountId(), nameOfDelegate)) {
        cvActivityLogService.getLoggerByStateExecutionId(context.getAccountId(), context.getStateExecutionInstanceId())
            .info("Delegate is not up yet, sleeping for 10 seconds.");
        log.info("Delegate is not up yet, sleeping for 30 seconds.");
        Thread.sleep(30000);
      }

      cvActivityLogService.getLoggerByStateExecutionId(context.getAccountId(), context.getStateExecutionInstanceId())
          .info("Delegate is up and running!");
      log.info("Delegate is up and running");
    } catch (IOException | InterruptedException e) {
      log.error("Exception while provisioning delegate", e);
      throw new WingsException("Exception while provisioning delegate", e);
    }
    return ExecutionResponse.builder().async(false).executionStatus(ExecutionStatus.SUCCESS).build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}
}

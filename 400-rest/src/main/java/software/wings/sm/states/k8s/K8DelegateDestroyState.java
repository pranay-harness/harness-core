package software.wings.sm.states.k8s;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.SweepingOutputInstance;
import io.harness.exception.WingsException;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.KubernetesHelperService;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;

import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.verification.CVActivityLogService;
import software.wings.sm.ExecutionContext;
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
import org.mongodb.morphia.annotations.Transient;

@FieldNameConstants(innerTypeName = "K8DelegateSpawnStateKeys")
@Attributes
@Slf4j
public class K8DelegateDestroyState extends State implements SweepingOutputStateMixin {
  private static final String kubectlBaseDir = "./client-tools/kubectl/";
  private static final String defaultKubectlVersion = "v1.13.2";
  private static String kubectlPath = kubectlBaseDir + defaultKubectlVersion;
  @Getter @Setter private String kubernetesConnectorId;
  @Getter @Setter private String delegateName;
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

  public K8DelegateDestroyState(String name) {
    super(name, StateType.DELEGATE_DESTROY.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
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

      Kubectl kubectl = Kubectl.client(kubectlPath + "/kubectl", kubeConf.getAbsolutePath());
      String finalCmd = kubectl.delete().filename(yamlFile.getAbsolutePath()).command();
      log.info("final command to be executed is " + finalCmd);
      OnDemandDelegateHelper.executeCommand(finalCmd, 10);
      cvActivityLogService.getLoggerByStateExecutionId(context.getAccountId(), context.getStateExecutionInstanceId())
          .info("Delegate delete command successful");
      // wait until delegate is up
      while (delegateService.checkDelegateConnectedByName(context.getAccountId(), nameOfDelegate)) {
        cvActivityLogService.getLoggerByStateExecutionId(context.getAccountId(), context.getStateExecutionInstanceId())
            .info("Delegate is still connected, sleeping for 10 seconds.");
        log.info("Delegate is not deleted yet, sleeping for 10 seconds.");
        Thread.sleep(10000);
      }
      cvActivityLogService.getLoggerByStateExecutionId(context.getAccountId(), context.getStateExecutionInstanceId())
          .info("Delegate has been deleted.");
      log.info("Delegate has been deleted");
    } catch (IOException | InterruptedException e) {
      log.error("Exception while provisioning delegate", e);
      throw new WingsException("Exception while provisioning delegate", e);
    }
    return ExecutionResponse.builder().async(false).executionStatus(ExecutionStatus.SUCCESS).build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}
}

package software.wings.service.impl.ci;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFetchFilesConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.ci.CIK8BuildTaskParams;
import software.wings.beans.ci.pod.CIContainerType;
import software.wings.beans.ci.pod.CIK8ContainerParams;
import software.wings.beans.ci.pod.CIK8PodParams;
import software.wings.beans.container.ImageDetails;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CIDelegateTaskHelperServiceImpl implements CIDelegateTaskHelperService {
  @Inject private SettingsService settingsService;
  @Inject private SecretManager secretManager;
  @Inject private DelegateService delegateService;
  private static final String accountId = "kmpySmUISimoRrJL6NL73w";

  /* TODO Send task params from CI directly instead of hardcoding it here require
       Before doing above, we have to expose api for fetching SettingAttribute and SecretManager
       and implement serialization
  */
  @Override
  public ResponseData setBuildEnv(CIK8BuildTaskParams cik8BuildTaskParams) {
    SettingAttribute cloudProvider = settingsService.getSettingAttributeByName(accountId, "gitRepo");
    GitFetchFilesConfig gitFetchFilesConfig = null;
    if (cloudProvider != null) {
      GitConfig gitConfig = (GitConfig) cloudProvider.getValue();
      List<EncryptedDataDetail> gitEncryptedDataDetails = secretManager.getEncryptionDetails(gitConfig);
      gitFetchFilesConfig = GitFetchFilesConfig.builder()
                                .encryptedDataDetails(gitEncryptedDataDetails)
                                .gitFileConfig(GitFileConfig.builder().branch("master").build())
                                .gitConfig(gitConfig)
                                .build();
    }
    SettingAttribute googleCloud = settingsService.getSettingAttributeByName(accountId, "kubernetes_clusterqqq");
    KubernetesClusterConfig kubernetesClusterConfig = null;
    List<EncryptedDataDetail> encryptedDataDetails = null;
    KubernetesConfig kubernetesConfig = null;
    if (googleCloud != null) {
      kubernetesClusterConfig = (KubernetesClusterConfig) googleCloud.getValue();
      kubernetesConfig = kubernetesClusterConfig.createKubernetesConfig(null);
      encryptedDataDetails = secretManager.getEncryptionDetails(kubernetesClusterConfig);
    }
    String stepExecVolumeName = "step-exec";
    String stepExecWorkingDir = "workspace";
    String podName = "pod1";
    String namespace = "default";

    ImageDetails imageDetails = ImageDetails.builder()
                                    .name("maven")
                                    .tag("3.6.3-jdk-8")
                                    .registryUrl("https://index.docker.io/v1/")
                                    .username("shubham149")
                                    .build();

    Map<String, String> map = new HashMap<>();
    map.put(stepExecVolumeName, "/step-exec");

    CIK8ContainerParams ctr = CIK8ContainerParams.builder()
                                  .name("build-setup")
                                  .containerType(CIContainerType.STEP_EXECUTOR)
                                  .commands(Arrays.asList("/bin/sh", "-c"))
                                  .args(Arrays.asList("trap : TERM INT; (while true; do sleep 1000; done) & wait"))
                                  .imageDetails(imageDetails)
                                  .volumeToMountPath(map)
                                  .build();

    CIK8PodParams<CIK8ContainerParams> podParams = CIK8PodParams.<CIK8ContainerParams>builder()
                                                       .name(podName)
                                                       .namespace(namespace)
                                                       .stepExecVolumeName(stepExecVolumeName)
                                                       .stepExecWorkingDir(stepExecWorkingDir)
                                                       .gitFetchFilesConfig(gitFetchFilesConfig)
                                                       .containerParamsList(Arrays.asList(ctr))
                                                       .build();
    try {
      ResponseData responseData = delegateService.executeTask(
          DelegateTask.builder()
              .accountId(accountId)
              .appId(GLOBAL_APP_ID)
              //   .async(false)
              .data(TaskData.builder()
                        .taskType(TaskType.CI_BUILD.name())
                        .parameters(new Object[] {CIK8BuildTaskParams.builder()
                                                      .gitFetchFilesConfig(gitFetchFilesConfig)
                                                      .encryptionDetails(encryptedDataDetails)
                                                      .kubernetesConfig(kubernetesConfig)
                                                      .cik8PodParams(podParams)
                                                      .build()})
                        .timeout(TimeUnit.SECONDS.toMillis(60))
                        .build())
              .build());
      logger.info(responseData.toString());
      return responseData;
    } catch (Exception e) {
      logger.error("Failed to execute delegate task", e);
    }
    return null;
  }
}

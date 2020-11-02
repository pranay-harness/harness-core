package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.git.model.GitRepositoryType;
import io.harness.k8s.model.HelmVersion;
import io.harness.logging.CommandExecutionStatus;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.GitConfig;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.helm.HelmDeployService;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.response.HelmCommandResponse;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.yaml.GitClient;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by anubhaw on 4/10/18.
 */
@Slf4j
public class HelmCommandValidation extends AbstractDelegateValidateTask {
  @Inject private HelmDeployService helmDeployService;
  @Inject private ContainerValidationHelper containerValidationHelper;
  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private GitClient gitClient;
  @Inject private EncryptionService encryptionService;

  public HelmCommandValidation(
      String delegateId, DelegateTaskPackage delegateTaskPackage, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTaskPackage, consumer);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<DelegateConnectionResult> validate() {
    HelmCommandRequest commandRequest = (HelmCommandRequest) getParameters()[0];
    ContainerServiceParams params = commandRequest.getContainerServiceParams();
    log.info("Running validation for task {} for cluster {}", delegateTaskId, params.getClusterName());

    boolean validated = false;
    try {
      String errorMsg = validateGitConnectivity(commandRequest);
      if (EmptyPredicate.isNotEmpty(errorMsg)) {
        log.warn("This delegate doesn't have Git connectivity, cannot perform helm deployment");
        return singletonList(
            DelegateConnectionResult.builder().criteria(getCriteria().get(0)).validated(false).build());
      }

      String configLocation =
          containerDeploymentDelegateHelper.createAndGetKubeConfigLocation(commandRequest.getContainerServiceParams());
      commandRequest.setKubeConfigLocation(configLocation);

      HelmCommandResponse helmCommandResponse = helmDeployService.ensureHelmInstalled(commandRequest);
      if (helmCommandResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
        validated = containerValidationHelper.validateContainerServiceParams(params);
        log.info("Helm containerServiceParams validation result. Validated: " + validated);
      }
    } catch (Exception e) {
      log.error("Helm validation failed", e);
    }

    log.info("HelmCommandValidation result. Validated: " + validated);

    return singletonList(
        DelegateConnectionResult.builder().criteria(getCriteria().get(0)).validated(validated).build());
  }

  @Override
  public List<String> getCriteria() {
    HelmCommandRequest helmCommandRequest = (HelmCommandRequest) getParameters()[0];
    return singletonList(
        getCriteria(helmCommandRequest.getContainerServiceParams(), helmCommandRequest.getHelmVersion()));
  }

  String getCriteria(ContainerServiceParams containerServiceParams, HelmVersion helmVersion) {
    if (helmVersion == HelmVersion.V3) {
      return "helm3: " + containerValidationHelper.getCriteria(containerServiceParams);
    }
    return "helm: " + containerValidationHelper.getCriteria(containerServiceParams);
  }

  private String validateGitConnectivity(HelmCommandRequest commandRequest) {
    GitConfig gitConfig = commandRequest.getGitConfig();
    List<EncryptedDataDetail> encryptionDetails = commandRequest.getEncryptedDataDetails();

    if (gitConfig == null) {
      return null;
    }

    try {
      encryptionService.decrypt(gitConfig, encryptionDetails, false);
    } catch (Exception e) {
      String errorMsg = "Failed to Decrypt gitConfig, RepoUrl: " + gitConfig.getRepoUrl();
      log.error(errorMsg, e);
      return errorMsg;
    }

    gitConfig.setGitRepoType(GitRepositoryType.YAML);
    return gitClient.validate(gitConfig);
  }
}

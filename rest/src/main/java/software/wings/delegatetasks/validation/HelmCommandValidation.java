package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static software.wings.beans.ErrorCode.UNREACHABLE_HOST;

import com.google.inject.Inject;

import io.harness.data.structure.EmptyPredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.GitConfig;
import software.wings.beans.GitConfig.GitRepositoryType;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.helm.HelmDeployService;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.response.HelmCommandResponse;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.yaml.GitClient;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by anubhaw on 4/10/18.
 */
public class HelmCommandValidation extends AbstractDelegateValidateTask {
  private static final Logger logger = LoggerFactory.getLogger(HelmCommandValidation.class);

  @Inject private transient HelmDeployService helmDeployService;
  @Inject private transient ContainerValidationHelper containerValidationHelper;
  @Inject private transient ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private transient GitClient gitClient;
  @Inject private transient EncryptionService encryptionService;

  public HelmCommandValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTask, consumer);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<DelegateConnectionResult> validate() {
    HelmCommandRequest commandRequest = (HelmCommandRequest) getParameters()[0];
    ContainerServiceParams params = commandRequest.getContainerServiceParams();
    logger.info("Running validation for task {} for cluster {}", delegateTaskId, params.getClusterName());

    boolean validated = false;
    try {
      String errorMsg = validateGitConnectivity(commandRequest);
      if (EmptyPredicate.isNotEmpty(errorMsg)) {
        logger.warn("This delegate doesn't have Git connectivity, cannot perform helm deployment");
        return singletonList(DelegateConnectionResult.builder()
                                 .criteria(getCriteria().get(0))
                                 .validated(!startsWith(errorMsg, UNREACHABLE_HOST.getDescription()))
                                 .build());
      }

      String configLocation =
          containerDeploymentDelegateHelper.createAndGetKubeConfigLocation(commandRequest.getContainerServiceParams());
      commandRequest.setKubeConfigLocation(configLocation);
      HelmCommandResponse helmCommandResponse = helmDeployService.ensureHelmCliAndTillerInstalled(commandRequest);
      if (helmCommandResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS)) {
        validated = containerValidationHelper.validateContainerServiceParams(params);
      }
    } catch (Exception e) {
      logger.error("Helm validation failed", e);
    }

    return singletonList(DelegateConnectionResult.builder().criteria(getCriteria(params)).validated(validated).build());
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(getCriteria(((HelmCommandRequest) getParameters()[0]).getContainerServiceParams()));
  }

  private String getCriteria(ContainerServiceParams containerServiceParams) {
    return "helm: " + containerValidationHelper.getCriteria(containerServiceParams);
  }

  private String validateGitConnectivity(HelmCommandRequest commandRequest) {
    GitConfig gitConfig = commandRequest.getGitConfig();
    List<EncryptedDataDetail> encryptionDetails = commandRequest.getEncryptedDataDetails();

    if (gitConfig == null) {
      return null;
    }

    try {
      encryptionService.decrypt(gitConfig, encryptionDetails);
    } catch (Exception e) {
      String errorMsg = new StringBuilder(64)
                            .append("Failed to Decrypt gitConfig, ")
                            .append("RepoUrl: ")
                            .append(gitConfig.getRepoUrl())
                            .toString();
      logger.error(errorMsg);
      return errorMsg;
    }

    gitConfig.setGitRepoType(GitRepositoryType.YAML);
    return gitClient.validate(gitConfig, true);
  }
}
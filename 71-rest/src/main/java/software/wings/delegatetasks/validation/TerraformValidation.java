package software.wings.delegatetasks.validation;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import software.wings.beans.GitConfig;
import software.wings.beans.delegation.TerraformProvisionParameters;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.yaml.GitClient;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class TerraformValidation extends AbstractDelegateValidateTask {
  public static final String TERRAFORM_CRITERIA = "terraform";
  @Inject private transient GitClient gitClient;
  @Inject private transient EncryptionService encryptionService;

  public TerraformValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTask, consumer);
  }

  private boolean validateTerraform() {
    logger.info("Running terraform validation for task {}", delegateTaskId);
    final ProcessExecutor processExecutor = new ProcessExecutor().command("/bin/sh", "-c", "terraform --version");
    boolean valid = false;
    try {
      final ProcessResult result = processExecutor.execute();
      valid = result.getExitValue() == 0;
    } catch (Exception e) {
      logger.error("Checking terraform version threw exception", e);
    }
    return valid;
  }

  private String getRepoUrl() {
    TerraformProvisionParameters terraformProvisionParameters = (TerraformProvisionParameters) getParameters()[0];
    if (terraformProvisionParameters.getSourceRepo() != null) {
      return terraformProvisionParameters.getSourceRepo().getRepoUrl();
    }
    return null;
  }

  private boolean validateGit() {
    TerraformProvisionParameters terraformProvisionParameters = (TerraformProvisionParameters) getParameters()[0];
    final GitConfig gitConfig = terraformProvisionParameters.getSourceRepo();
    if (gitConfig != null) {
      logger.info("Running git validation for task {} for repo [{}]", delegateTaskId, gitConfig.getRepoUrl());
      List<EncryptedDataDetail> encryptionDetails = terraformProvisionParameters.getSourceRepoEncryptionDetails();

      try {
        encryptionService.decrypt(gitConfig, encryptionDetails);
      } catch (Exception e) {
        logger.info("Failed to decrypt " + gitConfig.getRepoUrl(), e);
        return false;
      }
      return isEmpty(gitClient.validate(gitConfig));
    }
    return false;
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    boolean terraformValidate = validateTerraform();
    logger.info("Terraform validation result: {}", terraformValidate);
    if (!terraformValidate) {
      return singletonList(DelegateConnectionResult.builder().criteria(getCriteria().get(0)).validated(false).build());
    }
    boolean gitValidate = validateGit();
    logger.info("Git validation result: {}", gitValidate);
    return singletonList(
        DelegateConnectionResult.builder().criteria(getCriteria().get(0)).validated(gitValidate).build());
  }

  @Override
  public List<String> getCriteria() {
    return getRepoUrl() == null ? singletonList(TERRAFORM_CRITERIA)
                                : singletonList(TERRAFORM_CRITERIA + ":" + getRepoUrl());
  }
}
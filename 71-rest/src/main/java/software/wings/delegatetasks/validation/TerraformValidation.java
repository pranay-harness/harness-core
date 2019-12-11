package software.wings.delegatetasks.validation;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.Collections.singletonList;

import com.google.common.collect.Lists;
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

import java.util.Arrays;
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

  private DelegateConnectionResult makeGitValidationResultObject(boolean value, String url) {
    return DelegateConnectionResult.builder().criteria(createGitCriteria(url)).validated(value).build();
  }

  private String createGitCriteria(String url) {
    return "GIT: " + url;
  }

  private DelegateConnectionResult validateTerraform() {
    final ProcessExecutor processExecutor = new ProcessExecutor().command("/bin/sh", "-c", "terraform --version");
    boolean valid = false;
    try {
      final ProcessResult result = processExecutor.execute();
      valid = result.getExitValue() == 0;
    } catch (Exception e) {
      logger.error("Checking terraform version threw exception", e);
    }
    return DelegateConnectionResult.builder().criteria(TERRAFORM_CRITERIA).validated(valid).build();
  }

  private DelegateConnectionResult validateGit() {
    TerraformProvisionParameters terraformProvisionParameters = (TerraformProvisionParameters) getParameters()[0];
    final GitConfig gitConfig = terraformProvisionParameters.getSourceRepo();
    if (gitConfig != null) {
      logger.info("Running validation for task {} for repo [{}]", delegateTaskId, gitConfig.getRepoUrl());
      List<EncryptedDataDetail> encryptionDetails = terraformProvisionParameters.getSourceRepoEncryptionDetails();

      try {
        encryptionService.decrypt(gitConfig, encryptionDetails);
      } catch (Exception e) {
        logger.info("Failed to decrypt " + gitConfig.getRepoUrl(), e);
        return makeGitValidationResultObject(false, gitConfig.getRepoUrl());
      }
      return makeGitValidationResultObject(isEmpty(gitClient.validate(gitConfig)), gitConfig.getRepoUrl());
    }
    return null;
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    DelegateConnectionResult terraformValidate = validateTerraform();
    DelegateConnectionResult gitValidate = validateGit();

    final List<DelegateConnectionResult> resultList = Lists.newArrayList(terraformValidate);
    return addIfNotNull(resultList, gitValidate);
  }

  private <E> List<E> addIfNotNull(List<E> list, E object) {
    if (object != null) {
      list.add(object);
    }
    return list;
  }

  @Override
  public List<String> getCriteria() {
    TerraformProvisionParameters terraformProvisionParameters = (TerraformProvisionParameters) getParameters()[0];
    if (terraformProvisionParameters.getSourceRepo() != null) {
      return Arrays.asList(
          TERRAFORM_CRITERIA, createGitCriteria(terraformProvisionParameters.getSourceRepo().getRepoUrl()));
    }
    return singletonList(TERRAFORM_CRITERIA);
  }
}

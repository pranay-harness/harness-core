package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.ExplanationException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.api.TerraformExecutionData;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.GitOperationContext;
import software.wings.beans.NameValuePair;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.TerraformInputVariablesTaskResponse;
import software.wings.beans.delegation.TerraformProvisionParameters;
import software.wings.helpers.ext.terraform.TerraformConfigInspectClient.BLOCK_TYPE;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.TerraformConfigInspectService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.GitUtilsDelegate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class TerraformInputVariablesObtainTask extends AbstractDelegateRunnableTask {
  private static final String TERRAFORM_FILE_EXTENSION = ".tf";
  @Inject private GitService gitService;
  @Inject private EncryptionService encryptionService;
  @Inject private GitUtilsDelegate gitUtilsDelegate;
  @Inject private TerraformConfigInspectService terraformConfigInspectService;

  public TerraformInputVariablesObtainTask(String delegateId, DelegateTask delegateTask,
      Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public TerraformInputVariablesTaskResponse run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public TerraformInputVariablesTaskResponse run(Object[] parameters) {
    return run((TerraformProvisionParameters) parameters[0]);
  }

  private TerraformInputVariablesTaskResponse run(TerraformProvisionParameters parameters) {
    try {
      GitConfig gitConfig = parameters.getSourceRepo();
      if (isNotEmpty(parameters.getSourceRepoBranch())) {
        gitConfig.setBranch(parameters.getSourceRepoBranch());
      }

      GitOperationContext gitOperationContext = gitUtilsDelegate.cloneRepo(gitConfig,
          GitFileConfig.builder().connectorId(parameters.getSourceRepoSettingId()).build(),
          parameters.getSourceRepoEncryptionDetails());

      String absoluteModulePath =
          gitUtilsDelegate.resolveAbsoluteFilePath(gitOperationContext, parameters.getScriptPath());
      List<NameValuePair> variablesList = new ArrayList<>();

      if (noTfFiles(absoluteModulePath)) {
        throw new InvalidRequestException("No Terraform Files Found", WingsException.USER);
      }

      List<String> variables = terraformConfigInspectService.parseFieldsUnderCategory(
          absoluteModulePath, BLOCK_TYPE.VARIABLES.name().toLowerCase());

      if (variables != null) {
        variables.stream()
            .distinct()
            .map(variable -> NameValuePair.builder().name(variable).valueType(Type.TEXT.name()).build())
            .forEach(variablesList::add);
      }

      if (variablesList.isEmpty()) {
        throw new ExplanationException("No Terraform input variables found", null);
      }

      return TerraformInputVariablesTaskResponse.builder()
          .variablesList(variablesList)
          .terraformExecutionData(TerraformExecutionData.builder().executionStatus(ExecutionStatus.SUCCESS).build())
          .build();
    } catch (RuntimeException e) {
      logger.error("Terraform Input Variables Task Exception " + parameters, e);
      return TerraformInputVariablesTaskResponse.builder()
          .terraformExecutionData(TerraformExecutionData.builder()
                                      .executionStatus(ExecutionStatus.FAILED)
                                      .errorMessage(ExceptionUtils.getMessage(e))
                                      .build())
          .build();
    }
  }

  private boolean noTfFiles(String directory) {
    File dir = new File(directory);
    try {
      return FileUtils.listFiles(dir, new WildcardFileFilter("*.tf"), null).isEmpty();
    } catch (IllegalArgumentException e) {
      throw new InvalidRequestException(format("Could not read the specified "
                                                + "directory  \"%s\" for terraform files",
                                            dir.getName()),
          WingsException.USER);
    }
  }
}

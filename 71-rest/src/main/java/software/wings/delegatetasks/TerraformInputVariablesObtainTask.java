package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import com.bertramlabs.plugins.hcl4j.HCLParser;
import com.bertramlabs.plugins.hcl4j.HCLParserException;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.TaskParameters;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.TerraformExecutionData;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.GitConfig;
import software.wings.beans.NameValuePair;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.TerraformInputVariablesTaskResponse;
import software.wings.beans.delegation.TerraformProvisionParameters;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.beans.yaml.GitFile;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TerraformInputVariablesObtainTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(TerraformInputVariablesObtainTask.class);
  private static final String TERRAFORM_FILE_EXTENSION = ".tf";
  @Inject private GitService gitService;
  @Inject private EncryptionService encryptionService;

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
      encryptionService.decrypt(gitConfig, parameters.getSourceRepoEncryptionDetails());

      // TODO VS: do not fetch all files, only the ones mentioned in the main tf file
      String branch =
          isNotEmpty(parameters.getSourceRepoBranch()) ? parameters.getSourceRepoBranch() : gitConfig.getBranch();
      GitFetchFilesResult gitFetchFilesResult = gitService.fetchFilesByPath(gitConfig, UUID.randomUUID().toString(), "",
          branch, Collections.singletonList(parameters.getScriptPath()), true,
          Collections.singletonList(TERRAFORM_FILE_EXTENSION), false);

      HCLParser hclParser = new HCLParser();
      Set<NameValuePair> variablesList = new HashSet<>();

      boolean foundTerraformFiles = false;
      List<GitFile> files = gitFetchFilesResult.getFiles();
      if (isNotEmpty(files)) {
        for (GitFile file : files) {
          if (file.getFilePath().endsWith(TERRAFORM_FILE_EXTENSION)) {
            foundTerraformFiles = true;
            Map<String, Object> parsedContents;
            try {
              parsedContents = hclParser.parse(file.getFileContent());
            } catch (HCLParserException e) {
              logger.error("HCL Parser Exception for file [" + file.getFilePath() + "], " + gitConfig, e);
              throw new WingsException(ErrorCode.GENERAL_ERROR)
                  .addParam("message", "Invalid Terraform File [" + file.getFilePath() + "] : " + e.getMessage());
            }
            LinkedHashMap<String, Object> variables = (LinkedHashMap) parsedContents.get("variable");
            if (variables != null) {
              variables.keySet()
                  .stream()
                  .map(variable -> NameValuePair.builder().name(variable).valueType(Type.TEXT.name()).build())
                  .forEach(variablesList::add);
            }
          }
        }
      }
      if (!foundTerraformFiles) {
        throw new WingsException(ErrorCode.GENERAL_ERROR).addParam("message", "No Terraform Files Found");
      } else if (variablesList.isEmpty()) {
        throw new WingsException(ErrorCode.GENERAL_ERROR).addParam("message", "No Variables Found");
      }
      return TerraformInputVariablesTaskResponse.builder()
          .variablesList(new ArrayList<>(variablesList))
          .terraformExecutionData(TerraformExecutionData.builder().executionStatus(ExecutionStatus.SUCCESS).build())
          .build();
    } catch (RuntimeException | IOException e) {
      logger.error("Terraform Input Variables Task Exception " + parameters, e);
      return TerraformInputVariablesTaskResponse.builder()
          .terraformExecutionData(TerraformExecutionData.builder()
                                      .executionStatus(ExecutionStatus.FAILED)
                                      .errorMessage(ExceptionUtils.getMessage(e))
                                      .build())
          .build();
    }
  }
}

package software.wings.delegatetasks;

import com.google.inject.Inject;

import com.bertramlabs.plugins.hcl4j.HCLParser;
import com.bertramlabs.plugins.hcl4j.HCLParserException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.TerraformExecutionData;
import software.wings.beans.DelegateTask;
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
import software.wings.sm.ExecutionStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TerraformInputVariablesObtainTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(TerraformInputVariablesObtainTask.class);
  @Inject GitService gitService;
  @Inject private EncryptionService encryptionService;

  public TerraformInputVariablesObtainTask(String delegateId, DelegateTask delegateTask,
      Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public TerraformInputVariablesTaskResponse run(Object[] parameters) {
    return run((TerraformProvisionParameters) parameters[0]);
  }

  @SuppressFBWarnings("REC_CATCH_EXCEPTION")
  private TerraformInputVariablesTaskResponse run(TerraformProvisionParameters parameters) {
    try {
      GitConfig gitConfig = parameters.getSourceRepo();
      encryptionService.decrypt(gitConfig, parameters.getSourceRepoEncryptionDetails());

      // TODO VS: do not fetch all files, only the ones mentioned in the main tf file
      GitFetchFilesResult gitFetchFilesResult = gitService.fetchFilesByPath(gitConfig, UUID.randomUUID().toString(), "",
          gitConfig.getBranch(), Collections.singletonList(parameters.getScriptPath()), true);

      HCLParser hclParser = new HCLParser();
      Set<NameValuePair> variablesList = new HashSet<>();
      for (GitFile file : gitFetchFilesResult.getFiles()) {
        if (file.getFilePath().endsWith(".tf")) {
          Map<String, Object> parsedContents;
          try {
            parsedContents = hclParser.parse(file.getFileContent());
          } catch (HCLParserException e) {
            logger.error("HCL Parser Exception for file [" + file.getFilePath() + "], " + gitConfig, e);
            throw new WingsException(
                ErrorCode.GENERAL_ERROR, "Invalid Terraform File [" + file.getFilePath() + "] : " + e.getMessage());
          }
          LinkedHashMap<String, Object> variables = (LinkedHashMap) parsedContents.get("variable");
          variables.keySet()
              .stream()
              .map(variable -> NameValuePair.builder().name(variable).valueType(Type.TEXT.name()).build())
              .forEach(variablesList::add);
        }
      }
      return TerraformInputVariablesTaskResponse.builder()
          .variablesList(new ArrayList<>(variablesList))
          .terraformExecutionData(TerraformExecutionData.builder().executionStatus(ExecutionStatus.SUCCESS).build())
          .build();
    } catch (Exception e) {
      logger.error("Terraform Input Variables Task Exception " + parameters, e);
      return TerraformInputVariablesTaskResponse.builder()
          .terraformExecutionData(TerraformExecutionData.builder()
                                      .executionStatus(ExecutionStatus.FAILED)
                                      .errorMessage(e.getMessage())
                                      .build())
          .build();
    }
  }
}

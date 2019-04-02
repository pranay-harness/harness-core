package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import com.bertramlabs.plugins.hcl4j.HCLParser;
import com.bertramlabs.plugins.hcl4j.HCLParserException;
import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.task.TaskParameters;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.TerraformExecutionData;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.delegation.TerraformProvisionParameters;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.beans.yaml.GitFile;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TerraformFetchTargetsTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(TerraformFetchTargetsTask.class);
  @Inject private GitService gitService;
  @Inject private EncryptionService encryptionService;
  private static final String TERRAFORM_FILE_EXTENSION = ".tf";
  public TerraformFetchTargetsTask(String delegateId, DelegateTask delegateTask,
      Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public TerraformExecutionData run(Object[] parameters) {
    return run((TerraformProvisionParameters) parameters[0]);
  }

  private TerraformExecutionData run(TerraformProvisionParameters parameters) {
    try {
      encryptionService.decrypt(parameters.getSourceRepo(), parameters.getSourceRepoEncryptionDetails());
      GitFetchFilesResult gitFetchFilesResult =
          gitService.fetchFilesByPath(parameters.getSourceRepo(), UUID.randomUUID().toString(), null,
              parameters.getSourceRepoBranch(), Collections.singletonList(parameters.getScriptPath()), true,
              Collections.singletonList(TERRAFORM_FILE_EXTENSION), false);

      if (gitFetchFilesResult == null) {
        throw new WingsException(ErrorCode.UNKNOWN_ERROR, "Failed to fetch files from git");
      }

      List<GitFile> files = gitFetchFilesResult.getFiles();
      if (isEmpty(files)) {
        throw new WingsException(ErrorCode.GENERAL_ERROR).addParam("message", "No Terraform files found");
      }

      HCLParser hclParser = new HCLParser();
      List<String> targets = new ArrayList<>();
      for (GitFile file : files) {
        try {
          Map<String, Object> parsedContent = hclParser.parse(file.getFileContent());
          if (isNotEmpty(parsedContent)) {
            targets.addAll(getTargetModules(parsedContent));
            targets.addAll(getTargetResources(parsedContent));
          }
        } catch (HCLParserException | IOException e) {
          throw new WingsException(ErrorCode.GENERAL_ERROR, e)
              .addParam("message", "Failure in parsing file:" + file.getFilePath() + " for targets. " + e.getMessage());
        }
      }
      return TerraformExecutionData.builder().targets(targets).build();
    } catch (Exception e) {
      if (e instanceof WingsException) {
        throw e;
      } else {
        throw new WingsException(e);
      }
    }
  }

  @VisibleForTesting
  public List<String> getTargetResources(Map<String, Object> parsedContent) {
    List<String> targetResources = new ArrayList<>();
    Object object = parsedContent.get("resource");
    if (object != null) {
      Map<String, Map<String, Object>> resourceMap = (Map<String, Map<String, Object>>) object;
      for (Entry<String, Map<String, Object>> resource : resourceMap.entrySet()) {
        String resourceType = resource.getKey();
        if (resource.getValue() != null) {
          for (String resourceName : resource.getValue().keySet()) {
            targetResources.add(resourceType + "." + resourceName);
          }
        }
      }
    }
    return targetResources;
  }

  @VisibleForTesting
  public List<String> getTargetModules(Map<String, Object> parsedContent) {
    List<String> targetModules = new ArrayList<>();
    Object object = parsedContent.get("module");
    if (object != null) {
      Map<String, Object> moduleMap = (Map<String, Object>) object;
      for (String moduleName : moduleMap.keySet()) {
        targetModules.add("module." + moduleName);
      }
    }
    return targetModules;
  }

  @Override
  public ResponseData run(TaskParameters parameters) {
    return null;
  }
}

package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.UnexpectedException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.TerraformExecutionData;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.GitOperationContext;
import software.wings.beans.delegation.TerraformProvisionParameters;
import software.wings.helpers.ext.terraform.TerraformConfigInspectClient.BLOCK_TYPE;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.TerraformConfigInspectService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.GitUtilsDelegate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class TerraformFetchTargetsTask extends AbstractDelegateRunnableTask {
  @Inject private GitService gitService;
  @Inject private EncryptionService encryptionService;
  @Inject private GitUtilsDelegate gitUtilsDelegate;
  @Inject private TerraformConfigInspectService terraformConfigInspectService;

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
      GitConfig gitConfig = parameters.getSourceRepo();
      if (isNotEmpty(parameters.getSourceRepoBranch())) {
        gitConfig.setBranch(parameters.getSourceRepoBranch());
      }
      GitOperationContext gitOperationContext = gitUtilsDelegate.cloneRepo(gitConfig,
          GitFileConfig.builder().connectorId(parameters.getSourceRepoSettingId()).build(),
          parameters.getSourceRepoEncryptionDetails());

      String absoluteModulePath =
          gitUtilsDelegate.resolveAbsoluteFilePath(gitOperationContext, parameters.getScriptPath());
      List<String> targets = terraformConfigInspectService.parseFieldsUnderCategory(
          absoluteModulePath, BLOCK_TYPE.MANAGED_RESOURCES.name().toLowerCase());
      return TerraformExecutionData.builder().targets(targets).build();
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new UnexpectedException("Unknown failure while fetching targets", e);
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

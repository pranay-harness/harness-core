package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.settings.azureartifacts.AzureArtifactsConfig;
import software.wings.delegatetasks.collect.artifacts.AzureArtifactsCollectionTaskParameters;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class AzureArtifactsValidation extends AbstractDelegateValidateTask {
  public AzureArtifactsValidation(String delegateId, DelegateTaskPackage delegateTaskPackage,
      Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTaskPackage, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    Object[] params = getParameters();
    if (params != null && params.length == 1 && params[0] instanceof AzureArtifactsCollectionTaskParameters) {
      AzureArtifactsCollectionTaskParameters taskParams = (AzureArtifactsCollectionTaskParameters) params[0];
      return singletonList(taskParams.getAzureArtifactsConfig().getAzureDevopsUrl());
    }

    return singletonList(Arrays.stream(getParameters())
                             .filter(o -> o instanceof AzureArtifactsConfig)
                             .map(obj -> ((AzureArtifactsConfig) obj).getAzureDevopsUrl())
                             .findFirst()
                             .orElse(null));
  }
}

package software.wings.delegatetasks.validation;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static software.wings.beans.artifact.ArtifactStreamType.ACR;
import static software.wings.beans.artifact.ArtifactStreamType.AZURE_ARTIFACTS;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;

import io.harness.beans.DelegateTask;
import software.wings.beans.BambooConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.beans.settings.azureartifacts.AzureArtifactsConfig;
import software.wings.delegatetasks.buildsource.BuildSourceParameters;
import software.wings.settings.SettingValue;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by anubhaw on 7/19/18.
 */
public class BuildSourceTaskValidation extends AbstractDelegateValidateTask {
  private static final String ALWAYS_TRUE_CRITERIA = "ALWAYS_TRUE_CRITERIA";

  public BuildSourceTaskValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTask, consumer);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    List<String> criteria = getCriteria();
    if (criteria.contains(ALWAYS_TRUE_CRITERIA)) {
      return singletonList(DelegateConnectionResult.builder().criteria(ALWAYS_TRUE_CRITERIA).validated(true).build());
    } else {
      return super.validate();
    }
  }

  @Override
  public List<String> getCriteria() {
    Object[] parameters = getParameters();
    BuildSourceParameters buildSourceRequest = (BuildSourceParameters) parameters[0];

    SettingValue settingValue = buildSourceRequest.getSettingValue();
    ArtifactStreamAttributes artifactStreamAttributes = buildSourceRequest.getArtifactStreamAttributes();
    String artifactStreamType = artifactStreamAttributes.getArtifactStreamType();

    if (settingValue instanceof JenkinsConfig) {
      return asList(((JenkinsConfig) settingValue).getJenkinsUrl());
    } else if (settingValue instanceof BambooConfig) {
      return asList(((BambooConfig) settingValue).getBambooUrl());
    } else if (settingValue instanceof DockerConfig) {
      return asList(((DockerConfig) settingValue).getDockerRegistryUrl());
    } else if (settingValue instanceof NexusConfig) {
      return asList(((NexusConfig) settingValue).getNexusUrl());
    } else if (settingValue instanceof ArtifactoryConfig) {
      return asList(((ArtifactoryConfig) settingValue).getArtifactoryUrl());
    } else if (artifactStreamType.equals(GCR.name())) {
      return asList(getUrl(artifactStreamAttributes.getRegistryHostName()));
    } else if (artifactStreamType.equals(AZURE_ARTIFACTS.name())) {
      return asList(((AzureArtifactsConfig) settingValue).getAzureDevopsUrl());
    } else if (artifactStreamType.equals(ACR.name())) {
      final String default_server = "azure.microsoft.com";
      String loginServer = isNotEmpty(artifactStreamAttributes.getRegistryHostName())
          ? artifactStreamAttributes.getRegistryHostName()
          : default_server;
      return asList(getUrl(loginServer));
    }
    return asList(ALWAYS_TRUE_CRITERIA);
  }

  private String getUrl(String gcrHostName) {
    return "https://" + gcrHostName + (gcrHostName.endsWith("/") ? "" : "/");
  }
}

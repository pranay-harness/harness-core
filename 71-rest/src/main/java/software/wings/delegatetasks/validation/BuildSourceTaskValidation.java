package software.wings.delegatetasks.validation;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;
import static software.wings.common.Constants.ALWAYS_TRUE_CRITERIA;

import software.wings.beans.BambooConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.DockerConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.delegatetasks.buildsource.BuildSourceRequest;
import software.wings.settings.SettingValue;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by anubhaw on 7/19/18.
 */
public class BuildSourceTaskValidation extends AbstractDelegateValidateTask {
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
    BuildSourceRequest buildSourceRequest = (BuildSourceRequest) parameters[0];

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
    }
    return asList(ALWAYS_TRUE_CRITERIA);
  }

  private String getUrl(String gcrHostName) {
    return "https://" + gcrHostName + (gcrHostName.endsWith("/") ? "" : "/");
  }
}

package software.wings.service.impl.yaml.handler.artifactstream;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.HarnessException;
import software.wings.beans.FeatureName;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream.DockerArtifactStreamBuilder;
import software.wings.beans.artifact.DockerArtifactStream.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.service.intfc.FeatureFlagService;

import java.util.List;

/**
 * @author rktummala on 10/09/17
 */
@Singleton
public class DockerArtifactStreamYamlHandler extends ArtifactStreamYamlHandler<Yaml, DockerArtifactStream> {
  @Inject private FeatureFlagService featureFlagService;

  @Override
  public Yaml toYaml(DockerArtifactStream bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setImageName(bean.getImageName());
    return yaml;
  }

  @Override
  public DockerArtifactStream upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    String accountId = changeContext.getChange().getAccountId();
    String yamlFilePath = changeContext.getChange().getFilePath();
    DockerArtifactStream previous = get(accountId, yamlFilePath);

    if (!featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
      String appId =
          yamlHelper.getAppId(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
      String serviceId = yamlHelper.getServiceId(appId, changeContext.getChange().getFilePath());
      DockerArtifactStreamBuilder builder = DockerArtifactStream.builder().serviceId(serviceId).appId(appId);
      DockerArtifactStream dockerArtifactStream = toBean(accountId, builder, changeContext.getYaml(), appId);
      dockerArtifactStream.setName(yamlHelper.getArtifactStreamName(yamlFilePath));
      dockerArtifactStream.setSyncFromGit(changeContext.getChange().isSyncFromGit());

      if (previous != null) {
        dockerArtifactStream.setUuid(previous.getUuid());
        return (DockerArtifactStream) artifactStreamService.update(
            dockerArtifactStream, !dockerArtifactStream.isSyncFromGit());
      } else {
        return (DockerArtifactStream) artifactStreamService.createWithBinding(
            appId, dockerArtifactStream, !dockerArtifactStream.isSyncFromGit());
      }
    } else {
      if (changeContext.getYamlType().equals(YamlType.ARTIFACT_STREAM)) {
        String appId =
            yamlHelper.getAppId(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
        String serviceId = yamlHelper.getServiceId(appId, changeContext.getChange().getFilePath());
        DockerArtifactStreamBuilder builder = DockerArtifactStream.builder().serviceId(serviceId).appId(appId);
        DockerArtifactStream dockerArtifactStream = toBean(accountId, builder, changeContext.getYaml(), appId);
        dockerArtifactStream.setName(yamlHelper.getArtifactStreamName(yamlFilePath));
        dockerArtifactStream.setSyncFromGit(changeContext.getChange().isSyncFromGit());

        if (previous != null) {
          dockerArtifactStream.setUuid(previous.getUuid());
          return (DockerArtifactStream) artifactStreamService.update(
              dockerArtifactStream, !dockerArtifactStream.isSyncFromGit());
        } else {
          return (DockerArtifactStream) artifactStreamService.createWithBinding(
              appId, dockerArtifactStream, !dockerArtifactStream.isSyncFromGit());
        }
      } else {
        DockerArtifactStreamBuilder builder = DockerArtifactStream.builder().appId(GLOBAL_APP_ID);
        DockerArtifactStream dockerArtifactStream = toBean(accountId, builder, changeContext.getYaml(), GLOBAL_APP_ID);
        dockerArtifactStream.setName(yamlHelper.getArtifactStreamName(yamlFilePath));
        dockerArtifactStream.setSyncFromGit(changeContext.getChange().isSyncFromGit());
        if (previous != null) {
          dockerArtifactStream.setUuid(previous.getUuid());
          return (DockerArtifactStream) artifactStreamService.update(
              dockerArtifactStream, !dockerArtifactStream.isSyncFromGit());
        } else {
          return (DockerArtifactStream) artifactStreamService.createWithBinding(
              GLOBAL_APP_ID, dockerArtifactStream, !dockerArtifactStream.isSyncFromGit());
        }
      }
    }
  }

  @Override
  protected DockerArtifactStream getNewArtifactStreamObject() {
    return new DockerArtifactStream();
  }

  private DockerArtifactStream toBean(
      String accountId, DockerArtifactStreamBuilder builder, Yaml artifactStreamYaml, String appId) {
    return builder.settingId(getSettingId(accountId, appId, artifactStreamYaml.getServerName()))
        .imageName(artifactStreamYaml.getImageName())
        .build();
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}

package software.wings.service.impl.yaml.handler.artifactstream;

import static software.wings.utils.Util.isEmpty;

import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.ArtifactoryDockerArtifactStream;
import software.wings.beans.artifact.ArtifactoryDockerArtifactStream.Builder;
import software.wings.beans.artifact.ArtifactoryDockerArtifactStream.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * @author rktummala on 10/09/17
 */
public class ArtifactoryDockerArtifactStreamYamlHandler
    extends ArtifactStreamYamlHandler<ArtifactoryDockerArtifactStream.Yaml, ArtifactoryDockerArtifactStream> {
  @Override
  public ArtifactoryDockerArtifactStream.Yaml toYaml(ArtifactoryDockerArtifactStream artifactStream, String appId) {
    return ArtifactoryDockerArtifactStream.Yaml.Builder.aYaml()
        .withType(ArtifactStreamType.ARTIFACTORYDOCKER.name())
        .withSettingName(getSettingName(artifactStream.getSettingId()))
        .withAutoApproveForProduction(artifactStream.getAutoApproveForProduction())
        .withDockerImageName(artifactStream.getGroupId())
        .withImageName(artifactStream.getImageName())
        .withMetadataOnly(artifactStream.isMetadataOnly())
        .withRepositoryName(artifactStream.getJobname())
        .withSourceName(artifactStream.getSourceName())
        .build();
  }

  public ArtifactoryDockerArtifactStream updateFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }

    ArtifactoryDockerArtifactStream previous =
        getArtifactStream(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    Builder builder = previous.deepClone();
    setWithYamlValues(builder, changeContext.getYaml(), previous.getAppId());
    return (ArtifactoryDockerArtifactStream) artifactStreamService.update(builder.build());
  }

  private void setWithYamlValues(
      Builder builder, ArtifactoryDockerArtifactStream.Yaml artifactStreamYaml, String appId) {
    builder.withSettingId(getSettingId(appId, artifactStreamYaml.getSettingName()))
        .withAutoApproveForProduction(artifactStreamYaml.isAutoApproveForProduction())
        .withImageName(artifactStreamYaml.getImageName())
        .withMetadataOnly(artifactStreamYaml.isMetadataOnly())
        .withJobname(artifactStreamYaml.getRepositoryName())
        .withSourceName(artifactStreamYaml.getSourceName())
        .build();
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    ArtifactoryDockerArtifactStream.Yaml artifactStreamYaml = changeContext.getYaml();
    return !(isEmpty(artifactStreamYaml.getImageName()) || isEmpty(artifactStreamYaml.getRepositoryName())
        || isEmpty(artifactStreamYaml.getSourceName()) || isEmpty(artifactStreamYaml.getSettingName()));
  }

  @Override
  public ArtifactoryDockerArtifactStream createFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }

    String appId =
        yamlSyncHelper.getAppId(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    String serviceId = yamlSyncHelper.getServiceId(appId, changeContext.getChange().getFilePath());
    Builder builder = Builder.anArtifactoryDockerArtifactStream().withServiceId(serviceId).withAppId(appId);
    setWithYamlValues(builder, changeContext.getYaml(), appId);
    return (ArtifactoryDockerArtifactStream) artifactStreamService.create(builder.build());
  }

  @Override
  public Class getYamlClass() {
    return ArtifactoryDockerArtifactStream.Yaml.class;
  }
}
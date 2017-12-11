package software.wings.service.impl.yaml.handler.artifactstream;

import static software.wings.utils.Util.isEmpty;

import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.ArtifactoryArtifactStream;
import software.wings.beans.artifact.ArtifactoryArtifactStream.Builder;
import software.wings.beans.artifact.ArtifactoryArtifactStream.Yaml;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * @author rktummala on 10/09/17
 */
public class ArtifactoryArtifactStreamYamlHandler
    extends ArtifactStreamYamlHandler<ArtifactoryArtifactStream.Yaml, ArtifactoryArtifactStream> {
  @Override
  public ArtifactoryArtifactStream.Yaml toYaml(ArtifactoryArtifactStream artifactStream, String appId) {
    return ArtifactoryArtifactStream.Yaml.Builder.aYaml()
        .withType(ArtifactStreamType.ARTIFACTORY.name())
        .withSettingName(getSettingName(artifactStream.getSettingId()))
        .withArtifactPaths(artifactStream.getArtifactPaths())
        .withAutoApproveForProduction(artifactStream.getAutoApproveForProduction())
        .withArtifactPattern(artifactStream.getArtifactPattern())
        .withGroupId(artifactStream.getGroupId())
        .withImageName(artifactStream.getImageName())
        .withMetadataOnly(artifactStream.isMetadataOnly())
        .withRepositoryName(artifactStream.getJobname())
        .withSourceName(artifactStream.getSourceName())
        .build();
  }

  @Override
  public ArtifactoryArtifactStream upsertFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    if (changeContext.getChange().getChangeType().equals(ChangeType.ADD)) {
      return createFromYaml(changeContext, changeSetContext);
    } else {
      return updateFromYaml(changeContext, changeSetContext);
    }
  }

  @Override
  public ArtifactoryArtifactStream updateFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }

    ArtifactoryArtifactStream previous =
        getArtifactStream(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    Builder builder = previous.deepClone();
    setWithYamlValues(builder, changeContext.getYaml(), previous.getAppId());
    return (ArtifactoryArtifactStream) artifactStreamService.update(builder.build());
  }

  private void setWithYamlValues(
      ArtifactoryArtifactStream.Builder builder, ArtifactoryArtifactStream.Yaml artifactStreamYaml, String appId) {
    builder.withSettingId(getSettingId(appId, artifactStreamYaml.getSettingName()))
        .withArtifactPaths(artifactStreamYaml.getArtifactPaths())
        .withAutoApproveForProduction(artifactStreamYaml.isAutoApproveForProduction())
        .withArtifactPattern(artifactStreamYaml.getArtifactPattern())
        .withGroupId(artifactStreamYaml.getGroupId())
        .withImageName(artifactStreamYaml.getImageName())
        .withMetadataOnly(artifactStreamYaml.isMetadataOnly())
        .withJobname(artifactStreamYaml.getRepositoryName())
        .withSourceName(artifactStreamYaml.getSourceName())
        .build();
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    ArtifactoryArtifactStream.Yaml artifactStreamYaml = changeContext.getYaml();
    return !(isEmpty(artifactStreamYaml.getArtifactPaths()) || isEmpty(artifactStreamYaml.getArtifactPattern())
        || isEmpty(artifactStreamYaml.getGroupId()) || isEmpty(artifactStreamYaml.getImageName())
        || isEmpty(artifactStreamYaml.getRepositoryName()) || isEmpty(artifactStreamYaml.getSourceName()));
  }

  @Override
  public ArtifactoryArtifactStream createFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }

    String appId =
        yamlSyncHelper.getAppId(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    String serviceId = yamlSyncHelper.getServiceId(appId, changeContext.getChange().getFilePath());
    Builder builder = Builder.anArtifactoryArtifactStream().withServiceId(serviceId).withAppId(appId);
    setWithYamlValues(builder, changeContext.getYaml(), appId);
    return (ArtifactoryArtifactStream) artifactStreamService.create(builder.build());
  }

  @Override
  public Class getYamlClass() {
    return ArtifactoryArtifactStream.Yaml.class;
  }
}

package software.wings.service.impl.yaml.handler.artifactstream;

import static software.wings.utils.Util.isEmpty;

import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.GcrArtifactStream;
import software.wings.beans.artifact.GcrArtifactStream.Builder;
import software.wings.beans.artifact.GcrArtifactStream.Yaml;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * @author rktummala on 10/09/17
 */
public class GcrArtifactStreamYamlHandler extends ArtifactStreamYamlHandler<GcrArtifactStream.Yaml, GcrArtifactStream> {
  public GcrArtifactStream.Yaml toYaml(GcrArtifactStream artifactStream, String appId) {
    return GcrArtifactStream.Yaml.Builder.aYaml()
        .withType(ArtifactStreamType.GCR.name())
        .withSettingName(getSettingName(artifactStream.getSettingId()))
        .withAutoApproveForProduction(artifactStream.getAutoApproveForProduction())
        .withDockerImageName(artifactStream.getDockerImageName())
        .withRegistryHostName(artifactStream.getRegistryHostName())
        .withSourceName(artifactStream.getSourceName())
        .build();
  }

  @Override
  public GcrArtifactStream upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (changeContext.getChange().getChangeType().equals(ChangeType.ADD)) {
      return createFromYaml(changeContext, changeSetContext);
    } else {
      return updateFromYaml(changeContext, changeSetContext);
    }
  }

  @Override
  public GcrArtifactStream updateFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }

    GcrArtifactStream previous =
        getArtifactStream(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    Builder builder = previous.deepClone();
    setWithYamlValues(builder, changeContext.getYaml(), previous.getAppId());
    return (GcrArtifactStream) artifactStreamService.update(builder.build());
  }

  private void setWithYamlValues(
      GcrArtifactStream.Builder builder, GcrArtifactStream.Yaml artifactStreamYaml, String appId) {
    builder.withSettingId(getSettingId(appId, artifactStreamYaml.getSettingName()))
        .withAutoApproveForProduction(artifactStreamYaml.isAutoApproveForProduction())
        .withDockerImageName(artifactStreamYaml.getDockerImageName())
        .withRegistryHostName(artifactStreamYaml.getRegistryHostName())
        .withSourceName(artifactStreamYaml.getSourceName())
        .build();
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    GcrArtifactStream.Yaml artifactStreamYaml = changeContext.getYaml();
    return !(isEmpty(artifactStreamYaml.getDockerImageName()) || isEmpty(artifactStreamYaml.getRegistryHostName())
        || isEmpty(artifactStreamYaml.getSourceName()) || isEmpty(artifactStreamYaml.getSettingName()));
  }

  @Override
  public GcrArtifactStream createFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }

    String appId =
        yamlSyncHelper.getAppId(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    String serviceId = yamlSyncHelper.getServiceId(appId, changeContext.getChange().getFilePath());

    Builder builder = Builder.aGcrArtifactStream().withServiceId(serviceId).withAppId(appId);
    setWithYamlValues(builder, changeContext.getYaml(), appId);
    return (GcrArtifactStream) artifactStreamService.create(builder.build());
  }

  @Override
  public Class getYamlClass() {
    return GcrArtifactStream.Yaml.class;
  }
}

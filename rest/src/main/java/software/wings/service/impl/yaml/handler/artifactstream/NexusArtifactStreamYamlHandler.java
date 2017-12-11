package software.wings.service.impl.yaml.handler.artifactstream;

import static software.wings.utils.Util.isEmpty;

import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.beans.artifact.NexusArtifactStream.Builder;
import software.wings.beans.artifact.NexusArtifactStream.Yaml;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * @author rktummala on 10/09/17
 */
public class NexusArtifactStreamYamlHandler
    extends ArtifactStreamYamlHandler<NexusArtifactStream.Yaml, NexusArtifactStream> {
  @Override
  public NexusArtifactStream.Yaml toYaml(NexusArtifactStream artifactStream, String appId) {
    return NexusArtifactStream.Yaml.Builder.aYaml()
        .withType(ArtifactStreamType.NEXUS.name())
        .withSettingName(getSettingName(artifactStream.getSettingId()))
        .withArtifactPaths(artifactStream.getArtifactPaths())
        .withAutoApproveForProduction(artifactStream.getAutoApproveForProduction())
        .withMetadataOnly(artifactStream.isMetadataOnly())
        .withGroupId(artifactStream.getGroupId())
        .withRepositoryName(artifactStream.getJobname())
        .withSourceName(artifactStream.getSourceName())
        .build();
  }

  @Override
  public NexusArtifactStream upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (changeContext.getChange().getChangeType().equals(ChangeType.ADD)) {
      return createFromYaml(changeContext, changeSetContext);
    } else {
      return updateFromYaml(changeContext, changeSetContext);
    }
  }

  @Override
  public NexusArtifactStream updateFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }

    NexusArtifactStream previous =
        getArtifactStream(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    Builder builder = previous.deepClone();
    setWithYamlValues(builder, changeContext.getYaml(), previous.getAppId());
    return (NexusArtifactStream) artifactStreamService.update(builder.build());
  }

  private void setWithYamlValues(
      NexusArtifactStream.Builder builder, NexusArtifactStream.Yaml artifactStreamYaml, String appId) {
    builder.withSettingId(getSettingId(appId, artifactStreamYaml.getSettingName()))
        .withArtifactPaths(artifactStreamYaml.getArtifactPaths())
        .withAutoApproveForProduction(artifactStreamYaml.isAutoApproveForProduction())
        .withGroupId(artifactStreamYaml.getGroupId())
        .withJobname(artifactStreamYaml.getRepositoryName())
        .withMetadataOnly(artifactStreamYaml.isMetadataOnly())
        .withSourceName(artifactStreamYaml.getSourceName())
        .build();
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    NexusArtifactStream.Yaml artifactStreamYaml = changeContext.getYaml();
    return !(isEmpty(artifactStreamYaml.getArtifactPaths()) || isEmpty(artifactStreamYaml.getGroupId())
        || isEmpty(artifactStreamYaml.getRepositoryName()) || isEmpty(artifactStreamYaml.getSourceName())
        || isEmpty(artifactStreamYaml.getSettingName()));
  }

  @Override
  public NexusArtifactStream createFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }

    String appId =
        yamlSyncHelper.getAppId(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    String serviceId = yamlSyncHelper.getServiceId(appId, changeContext.getChange().getFilePath());
    Builder builder = Builder.aNexusArtifactStream().withServiceId(serviceId).withAppId(appId);
    setWithYamlValues(builder, changeContext.getYaml(), appId);
    return (NexusArtifactStream) artifactStreamService.create(builder.build());
  }

  @Override
  public Class getYamlClass() {
    return NexusArtifactStream.Yaml.class;
  }
}

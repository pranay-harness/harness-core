package software.wings.service.impl.yaml.handler.artifactstream;

import static software.wings.utils.Util.isEmpty;

import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.BambooArtifactStream;
import software.wings.beans.artifact.BambooArtifactStream.Builder;
import software.wings.beans.artifact.BambooArtifactStream.Yaml;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * @author rktummala on 10/09/17
 */
public class BambooArtifactStreamYamlHandler
    extends ArtifactStreamYamlHandler<BambooArtifactStream.Yaml, BambooArtifactStream> {
  public BambooArtifactStream.Yaml toYaml(BambooArtifactStream artifactStream, String appId) {
    return BambooArtifactStream.Yaml.Builder.aYaml()
        .withType(ArtifactStreamType.BAMBOO.name())
        .withSettingName(getSettingName(artifactStream.getSettingId()))
        .withArtifactPaths(artifactStream.getArtifactPaths())
        .withAutoApproveForProduction(artifactStream.getAutoApproveForProduction())
        .withMetadataOnly(artifactStream.isMetadataOnly())
        .withPlanName(artifactStream.getJobname())
        .withSourceName(artifactStream.getSourceName())
        .build();
  }

  @Override
  public BambooArtifactStream upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (changeContext.getChange().getChangeType().equals(ChangeType.ADD)) {
      return createFromYaml(changeContext, changeSetContext);
    } else {
      return updateFromYaml(changeContext, changeSetContext);
    }
  }

  public BambooArtifactStream updateFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }

    BambooArtifactStream previous =
        getArtifactStream(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    Builder builder = previous.deepClone();
    setWithYamlValues(builder, changeContext.getYaml(), previous.getAppId());
    return (BambooArtifactStream) artifactStreamService.update(builder.build());
  }

  private void setWithYamlValues(
      BambooArtifactStream.Builder builder, BambooArtifactStream.Yaml artifactStreamYaml, String appId) {
    builder.withSettingId(getSettingId(appId, artifactStreamYaml.getSettingName()))
        .withArtifactPaths(artifactStreamYaml.getArtifactPaths())
        .withAutoApproveForProduction(artifactStreamYaml.isAutoApproveForProduction())
        .withJobname(artifactStreamYaml.getPlanName())
        .withMetadataOnly(artifactStreamYaml.isMetadataOnly())
        .withSourceName(artifactStreamYaml.getSourceName())
        .build();
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    BambooArtifactStream.Yaml artifactStreamYaml = changeContext.getYaml();
    return !(isEmpty(artifactStreamYaml.getArtifactPaths()) || isEmpty(artifactStreamYaml.getPlanName())
        || isEmpty(artifactStreamYaml.getSourceName()) || isEmpty(artifactStreamYaml.getSettingName()));
  }

  @Override
  public BambooArtifactStream createFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }

    String appId =
        yamlSyncHelper.getAppId(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    String serviceId = yamlSyncHelper.getServiceId(appId, changeContext.getChange().getFilePath());
    Builder builder = Builder.aBambooArtifactStream().withServiceId(serviceId).withAppId(appId);
    setWithYamlValues(builder, changeContext.getYaml(), appId);
    return (BambooArtifactStream) artifactStreamService.create(builder.build());
  }

  @Override
  public Class getYamlClass() {
    return BambooArtifactStream.Yaml.class;
  }
}

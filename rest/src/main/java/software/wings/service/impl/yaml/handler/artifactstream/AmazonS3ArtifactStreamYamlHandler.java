package software.wings.service.impl.yaml.handler.artifactstream;

import static software.wings.beans.artifact.ArtifactStreamType.AMAZON_S3;
import static software.wings.utils.Util.isEmpty;

import software.wings.beans.artifact.AmazonS3ArtifactStream;
import software.wings.beans.artifact.AmazonS3ArtifactStream.Builder;
import software.wings.beans.artifact.AmazonS3ArtifactStream.Yaml;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * @author rktummala on 10/09/17
 */
public class AmazonS3ArtifactStreamYamlHandler
    extends ArtifactStreamYamlHandler<AmazonS3ArtifactStream.Yaml, AmazonS3ArtifactStream> {
  @Override
  public AmazonS3ArtifactStream.Yaml toYaml(AmazonS3ArtifactStream artifactStream, String appId) {
    return AmazonS3ArtifactStream.Yaml.Builder.aYaml()
        .withType(AMAZON_S3.name())
        .withSettingName(getSettingName(artifactStream.getSettingId()))
        .withArtifactPaths(artifactStream.getArtifactPaths())
        .withAutoApproveForProduction(artifactStream.getAutoApproveForProduction())
        .withBucketName(artifactStream.getJobname())
        .withMetadataOnly(artifactStream.isMetadataOnly())
        .withSourceName(artifactStream.getSourceName())
        .build();
  }

  @Override
  public AmazonS3ArtifactStream upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (changeContext.getChange().getChangeType().equals(ChangeType.ADD)) {
      return createFromYaml(changeContext, changeSetContext);
    } else {
      return updateFromYaml(changeContext, changeSetContext);
    }
  }

  @Override
  public AmazonS3ArtifactStream updateFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }

    AmazonS3ArtifactStream previous =
        getArtifactStream(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    Builder builder = previous.deepClone();
    setWithYamlValues(builder, changeContext.getYaml(), previous.getAppId());
    return (AmazonS3ArtifactStream) artifactStreamService.update(builder.build());
  }

  private void setWithYamlValues(
      AmazonS3ArtifactStream.Builder builder, AmazonS3ArtifactStream.Yaml artifactStreamYaml, String appId) {
    builder.withSettingId(getSettingId(appId, artifactStreamYaml.getSettingName()))
        .withArtifactPaths(artifactStreamYaml.getArtifactPaths())
        .withAutoApproveForProduction(artifactStreamYaml.isAutoApproveForProduction())
        .withJobname(artifactStreamYaml.getBucketName())
        .withMetadataOnly(artifactStreamYaml.isMetadataOnly())
        .withSourceName(artifactStreamYaml.getSourceName())
        .build();
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    AmazonS3ArtifactStream.Yaml artifactStreamYaml = changeContext.getYaml();
    return !(isEmpty(artifactStreamYaml.getArtifactPaths()) || isEmpty(artifactStreamYaml.getBucketName())
        || isEmpty(artifactStreamYaml.getSourceName()) || isEmpty(artifactStreamYaml.getSettingName()));
  }

  @Override
  public AmazonS3ArtifactStream createFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }

    String yamlFilePath = changeContext.getChange().getFilePath();
    String appId = yamlSyncHelper.getAppId(changeContext.getChange().getAccountId(), yamlFilePath);
    String serviceId = yamlSyncHelper.getServiceId(appId, yamlFilePath);
    Builder builder = Builder.anAmazonS3ArtifactStream().withServiceId(serviceId).withAppId(appId);
    setWithYamlValues(builder, changeContext.getYaml(), appId);
    return (AmazonS3ArtifactStream) artifactStreamService.create(builder.build());
  }

  @Override
  public Class getYamlClass() {
    return AmazonS3ArtifactStream.Yaml.class;
  }
}

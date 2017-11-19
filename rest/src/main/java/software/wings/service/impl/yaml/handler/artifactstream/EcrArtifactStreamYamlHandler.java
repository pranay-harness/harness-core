package software.wings.service.impl.yaml.handler.artifactstream;

import static software.wings.utils.Util.isEmpty;

import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.beans.artifact.EcrArtifactStream.Builder;
import software.wings.beans.artifact.EcrArtifactStream.Yaml;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * @author rktummala on 10/09/17
 */
public class EcrArtifactStreamYamlHandler extends ArtifactStreamYamlHandler<EcrArtifactStream.Yaml, EcrArtifactStream> {
  public EcrArtifactStream.Yaml toYaml(EcrArtifactStream artifactStream, String appId) {
    return EcrArtifactStream.Yaml.Builder.aYaml()
        .withType(ArtifactStreamType.ECR.name())
        .withSettingName(getSettingName(artifactStream.getSettingId()))
        .withAutoApproveForProduction(artifactStream.getAutoApproveForProduction())
        .withImageName(artifactStream.getImageName())
        .withRegion(artifactStream.getRegion())
        .withSourceName(artifactStream.getSourceName())
        .build();
  }

  @Override
  public EcrArtifactStream upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (changeContext.getChange().getChangeType().equals(ChangeType.ADD)) {
      return createFromYaml(changeContext, changeSetContext);
    } else {
      return updateFromYaml(changeContext, changeSetContext);
    }
  }

  public EcrArtifactStream updateFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }

    EcrArtifactStream previous =
        getArtifactStream(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    Builder builder = previous.deepClone();
    setWithYamlValues(builder, changeContext.getYaml(), previous.getAppId());
    return (EcrArtifactStream) artifactStreamService.update(builder.build());
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    EcrArtifactStream.Yaml artifactStreamYaml = changeContext.getYaml();
    return !(isEmpty(artifactStreamYaml.getImageName()) || isEmpty(artifactStreamYaml.getRegion())
        || isEmpty(artifactStreamYaml.getSourceName()) || isEmpty(artifactStreamYaml.getSettingName()));
  }

  @Override
  public EcrArtifactStream createFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }

    String appId = null;
    String serviceId = null;
    Builder builder = Builder.anEcrArtifactStream().withServiceId(serviceId).withAppId(appId);
    setWithYamlValues(builder, changeContext.getYaml(), appId);
    return (EcrArtifactStream) artifactStreamService.create(builder.build());
  }

  private void setWithYamlValues(
      EcrArtifactStream.Builder builder, EcrArtifactStream.Yaml artifactStreamYaml, String appId) {
    builder.withSettingId(getSettingId(appId, artifactStreamYaml.getSettingName()))
        .withAutoApproveForProduction(artifactStreamYaml.isAutoApproveForProduction())
        .withImageName(artifactStreamYaml.getImageName())
        .withRegion(artifactStreamYaml.getRegion())
        .withSourceName(artifactStreamYaml.getSourceName())
        .build();
  }

  @Override
  public Class getYamlClass() {
    return EcrArtifactStream.Yaml.class;
  }
}

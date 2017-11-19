package software.wings.service.impl.yaml.handler.artifactstream;

import static software.wings.utils.Util.isEmpty;

import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream.Builder;
import software.wings.beans.artifact.DockerArtifactStream.Yaml;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * @author rktummala on 10/09/17
 */
public class DockerArtifactStreamYamlHandler
    extends ArtifactStreamYamlHandler<DockerArtifactStream.Yaml, DockerArtifactStream> {
  public DockerArtifactStream.Yaml toYaml(DockerArtifactStream artifactStream, String appId) {
    return DockerArtifactStream.Yaml.Builder.aYaml()
        .withType(ArtifactStreamType.DOCKER.name())
        .withSettingName(getSettingName(artifactStream.getSettingId()))
        .withSettingName(getSettingName(artifactStream.getSettingId()))
        .withAutoApproveForProduction(artifactStream.getAutoApproveForProduction())
        .withImageName(artifactStream.getImageName())
        .withSourceName(artifactStream.getSourceName())
        .build();
  }

  @Override
  public DockerArtifactStream upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (changeContext.getChange().getChangeType().equals(ChangeType.ADD)) {
      return createFromYaml(changeContext, changeSetContext);
    } else {
      return updateFromYaml(changeContext, changeSetContext);
    }
  }

  public DockerArtifactStream updateFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }

    DockerArtifactStream previous =
        getArtifactStream(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    Builder builder = previous.deepClone();
    setWithYamlValues(builder, changeContext.getYaml(), previous.getAppId());
    return (DockerArtifactStream) artifactStreamService.update(builder.build());
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    DockerArtifactStream.Yaml artifactStreamYaml = changeContext.getYaml();
    return !(isEmpty(artifactStreamYaml.getImageName()) || isEmpty(artifactStreamYaml.getSourceName())
        || isEmpty(artifactStreamYaml.getSettingName()));
  }

  @Override
  public DockerArtifactStream createFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }

    String appId =
        yamlSyncHelper.getAppId(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    String serviceId = yamlSyncHelper.getServiceId(appId, changeContext.getChange().getFilePath());
    Builder builder = Builder.aDockerArtifactStream().withServiceId(serviceId).withAppId(appId);
    setWithYamlValues(builder, changeContext.getYaml(), appId);
    return (DockerArtifactStream) artifactStreamService.create(builder.build());
  }

  private void setWithYamlValues(
      DockerArtifactStream.Builder builder, DockerArtifactStream.Yaml artifactStreamYaml, String appId) {
    builder.withSettingId(getSettingId(appId, artifactStreamYaml.getSettingName()))
        .withAutoApproveForProduction(artifactStreamYaml.isAutoApproveForProduction())
        .withImageName(artifactStreamYaml.getImageName())
        .withSourceName(artifactStreamYaml.getSourceName())
        .build();
  }

  @Override
  public Class getYamlClass() {
    return DockerArtifactStream.Yaml.class;
  }
}

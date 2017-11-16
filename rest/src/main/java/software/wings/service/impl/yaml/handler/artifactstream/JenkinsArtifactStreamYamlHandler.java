package software.wings.service.impl.yaml.handler.artifactstream;

import static software.wings.utils.Util.isEmpty;

import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream.Builder;
import software.wings.beans.artifact.JenkinsArtifactStream.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * @author rktummala on 10/09/17
 */
public class JenkinsArtifactStreamYamlHandler
    extends ArtifactStreamYamlHandler<JenkinsArtifactStream.Yaml, JenkinsArtifactStream> {
  @Override
  public JenkinsArtifactStream.Yaml toYaml(JenkinsArtifactStream artifactStream, String appId) {
    return JenkinsArtifactStream.Yaml.Builder.aYaml()
        .withType(ArtifactStreamType.JENKINS.name())
        .withSettingName(getSettingName(artifactStream.getSettingId()))
        .withArtifactPaths(artifactStream.getArtifactPaths())
        .withAutoApproveForProduction(artifactStream.getAutoApproveForProduction())
        .withMetadataOnly(artifactStream.isMetadataOnly())
        .withJobName(artifactStream.getJobname())
        .withSourceName(artifactStream.getSourceName())
        .build();
  }

  @Override
  public JenkinsArtifactStream updateFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }

    JenkinsArtifactStream previous =
        getArtifactStream(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    Builder builder = previous.deepClone();
    setWithYamlValues(builder, changeContext.getYaml(), previous.getAppId());
    return (JenkinsArtifactStream) artifactStreamService.update(builder.build());
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    JenkinsArtifactStream.Yaml artifactStreamYaml = changeContext.getYaml();
    return !(isEmpty(artifactStreamYaml.getArtifactPaths()) || isEmpty(artifactStreamYaml.getJobName())
        || isEmpty(artifactStreamYaml.getSourceName()) || isEmpty(artifactStreamYaml.getSettingName()));
  }

  @Override
  public JenkinsArtifactStream createFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }

    String appId =
        yamlSyncHelper.getAppId(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    String serviceId = yamlSyncHelper.getServiceId(appId, changeContext.getChange().getFilePath());

    Builder builder = Builder.aJenkinsArtifactStream().withServiceId(serviceId).withAppId(appId);
    setWithYamlValues(builder, changeContext.getYaml(), appId);
    return (JenkinsArtifactStream) artifactStreamService.create(builder.build());
  }

  private void setWithYamlValues(
      JenkinsArtifactStream.Builder builder, JenkinsArtifactStream.Yaml artifactStreamYaml, String appId) {
    builder.withArtifactPaths(artifactStreamYaml.getArtifactPaths())
        .withSettingId(getSettingId(appId, artifactStreamYaml.getSettingName()))
        .withAutoApproveForProduction(artifactStreamYaml.isAutoApproveForProduction())
        .withJobname(artifactStreamYaml.getJobName())
        .withMetadataOnly(artifactStreamYaml.isMetadataOnly())
        .withSourceName(artifactStreamYaml.getSourceName())
        .build();
  }

  @Override
  public Class getYamlClass() {
    return JenkinsArtifactStream.Yaml.class;
  }
}

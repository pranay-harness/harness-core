package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Singleton;

import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream.Builder;
import software.wings.beans.artifact.DockerArtifactStream.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * @author rktummala on 10/09/17
 */
@Singleton
public class DockerArtifactStreamYamlHandler
    extends ArtifactStreamYamlHandler<DockerArtifactStream.Yaml, DockerArtifactStream> {
  public DockerArtifactStream.Yaml toYaml(DockerArtifactStream bean, String appId) {
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

    String appId =
        yamlHelper.getAppId(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    String serviceId = yamlHelper.getServiceId(appId, changeContext.getChange().getFilePath());
    Builder builder = Builder.aDockerArtifactStream().withServiceId(serviceId).withAppId(appId);
    toBean(accountId, builder, changeContext.getYaml(), appId);
    if (previous != null) {
      builder.withUuid(previous.getUuid());
      return (DockerArtifactStream) artifactStreamService.update(builder.build());
    } else {
      return (DockerArtifactStream) artifactStreamService.create(builder.build());
    }
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    DockerArtifactStream.Yaml artifactStreamYaml = changeContext.getYaml();
    return !(isEmpty(artifactStreamYaml.getImageName()) || isEmpty(artifactStreamYaml.getServerName()));
  }

  @Override
  protected DockerArtifactStream getNewArtifactStreamObject() {
    return new DockerArtifactStream();
  }

  private void toBean(String accountId, Builder builder, Yaml artifactStreamYaml, String appId) {
    builder.withSettingId(getSettingId(accountId, appId, artifactStreamYaml.getServerName()))
        .withImageName(artifactStreamYaml.getImageName())
        .build();
  }

  @Override
  public Class getYamlClass() {
    return DockerArtifactStream.Yaml.class;
  }
}

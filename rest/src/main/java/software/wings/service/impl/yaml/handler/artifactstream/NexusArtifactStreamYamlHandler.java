package software.wings.service.impl.yaml.handler.artifactstream;

import static software.wings.utils.Util.isEmpty;

import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.beans.artifact.NexusArtifactStream.Yaml;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

/**
 * @author rktummala on 10/09/17
 */
public class NexusArtifactStreamYamlHandler
    extends ArtifactStreamYamlHandler<NexusArtifactStream.Yaml, NexusArtifactStream> {
  @Override
  public Yaml toYaml(NexusArtifactStream bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setArtifactPaths(bean.getArtifactPaths());
    yaml.setGroupId(bean.getGroupId());
    yaml.setRepositoryName(bean.getJobname());
    return yaml;
  }

  protected void toBean(NexusArtifactStream bean, ChangeContext<Yaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    Yaml yaml = changeContext.getYaml();
    bean.setArtifactPaths(yaml.getArtifactPaths());
    bean.setGroupId(yaml.getGroupId());
    bean.setJobname(yaml.getRepositoryName());
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    Yaml artifactStreamYaml = changeContext.getYaml();
    return !(isEmpty(artifactStreamYaml.getArtifactPaths()) || isEmpty(artifactStreamYaml.getGroupId())
        || isEmpty(artifactStreamYaml.getRepositoryName()) || isEmpty(artifactStreamYaml.getArtifactServerName()));
  }

  @Override
  protected NexusArtifactStream getNewArtifactStreamObject() {
    return new NexusArtifactStream();
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}

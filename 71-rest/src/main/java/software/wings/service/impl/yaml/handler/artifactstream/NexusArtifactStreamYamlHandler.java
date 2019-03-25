package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Singleton;

import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.beans.artifact.NexusArtifactStream.Yaml;
import software.wings.beans.yaml.ChangeContext;

/**
 * @author rktummala on 10/09/17
 */
@Singleton
public class NexusArtifactStreamYamlHandler
    extends ArtifactStreamYamlHandler<NexusArtifactStream.Yaml, NexusArtifactStream> {
  @Override
  public Yaml toYaml(NexusArtifactStream bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setRepositoryName(bean.getJobname());
    yaml.setArtifactPaths(bean.getArtifactPaths());
    if (isNotEmpty(bean.getArtifactPaths())) {
      yaml.setGroupId(bean.getGroupId());
    } else {
      yaml.setImageName(bean.getImageName());
      yaml.setDockerRegistryUrl(bean.getDockerRegistryUrl());
    }
    return yaml;
  }

  protected void toBean(NexusArtifactStream bean, ChangeContext<Yaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    Yaml yaml = changeContext.getYaml();
    if (isNotEmpty(yaml.getArtifactPaths())) {
      bean.setArtifactPaths(yaml.getArtifactPaths());
      bean.setGroupId(yaml.getGroupId());
    } else {
      bean.setImageName(yaml.getImageName());
      bean.setDockerRegistryUrl(yaml.getDockerRegistryUrl());
    }
    bean.setJobname(yaml.getRepositoryName());
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

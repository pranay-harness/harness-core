package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Singleton;

import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream.Yaml;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

/**
 * @author rktummala on 10/09/17
 */
@Singleton
public class JenkinsArtifactStreamYamlHandler
    extends ArtifactStreamYamlHandler<JenkinsArtifactStream.Yaml, JenkinsArtifactStream> {
  @Override
  public Yaml toYaml(JenkinsArtifactStream bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setArtifactPaths(bean.getArtifactPaths());
    yaml.setJobName(bean.getJobname());
    return yaml;
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    Yaml artifactStreamYaml = changeContext.getYaml();
    return !(isEmpty(artifactStreamYaml.getArtifactPaths()) || isEmpty(artifactStreamYaml.getJobName())
        || isEmpty(artifactStreamYaml.getServerName()));
  }

  @Override
  protected JenkinsArtifactStream getNewArtifactStreamObject() {
    return new JenkinsArtifactStream();
  }

  protected void toBean(JenkinsArtifactStream bean, ChangeContext<Yaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    Yaml yaml = changeContext.getYaml();
    bean.setArtifactPaths(yaml.getArtifactPaths());
    bean.setJobname(yaml.getJobName());
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}

package software.wings.helpers.ext.docker;

import software.wings.beans.DockerConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

/**
 * Created by anubhaw on 1/6/17.
 */
public interface DockerRegistryService {
  public List<BuildDetails> getBuilds(DockerConfig dockerConfig, String imageName, int maxNumberOfBuilds);
  public BuildDetails getLastSuccessfulBuild(DockerConfig dockerConfig, String imageName);
}

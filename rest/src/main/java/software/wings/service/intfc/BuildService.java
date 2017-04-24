package software.wings.service.intfc;

import java.util.Set;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;
import java.util.Map;

/**
 * Created by peeyushaggarwal on 5/13/16.
 *
 * @param <T> the type parameter
 */
public interface BuildService<T> {
  /**
   * Gets builds.
   *
   * @param appId                     the app id
   * @param artifactStreamAttributes the build service request params
   * @param config                    the jenkins config
   * @return the builds
   */
  List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes, T config);

  /**
   * Gets jobs.
   *
   * @param jenkinsConfig the jenkins setting id
   * @return the jobs
   */
  List<String> getJobs(T jenkinsConfig);

  /**
   * Gets artifact paths.
   *
   * @param jobName the job name
   * @param  groupId the Group Id
   * @param config  the jenkins config
   * @return the artifact paths
   */
  List<String> getArtifactPaths(String jobName, String groupId, T config);

  /**
   * Gets last successful build.
   *
   * @param appId                     the app id
   * @param artifactStreamAttributes the build service request params
   * @param config                    the jenkins config
   * @return the last successful build
   */
  BuildDetails getLastSuccessfulBuild(String appId, ArtifactStreamAttributes artifactStreamAttributes, T config);

  /**
   * Gets plans.
   *
   * @param config the jenkins config
   * @return the plans
   */
  Map<String, String> getPlans(T config);

  /**
   * Gets group Id paths.
   *
   * @param repoType   The repo type
   * @param config  the config
   * @return the groupId paths
   */
  List<String> getGroupIds(String repoType, T config);

  /**
   * Validates Artifact Server
   * @param config
   * @throws software.wings.exception.WingsException if not valid
   */
  void validateArtifactServer(T config);

  /**
   * Validates Artifact Stream
   * @param artifactStreamAttributes
   * @throws software.wings.exception.WingsException if not valid
   */
  void validateArtifactSource(T config, ArtifactStreamAttributes artifactStreamAttributes);
}

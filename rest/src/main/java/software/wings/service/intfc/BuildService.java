package software.wings.service.intfc;

import software.wings.beans.artifact.ArtifactStream;
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
   * @param appId          the app id
   * @param artifactStream the artifact stream
   * @param config         the jenkins config
   * @return the builds
   */
  List<BuildDetails> getBuilds(String appId, ArtifactStream artifactStream, T config);

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
   * @param config  the jenkins config
   * @return the artifact paths
   */
  List<String> getArtifactPaths(String jobName, T config);

  /**
   * Gets last successful build.
   *
   * @param appId          the app id
   * @param artifactStream the artifact stream
   * @param config         the jenkins config
   * @return the last successful build
   */
  BuildDetails getLastSuccessfulBuild(String appId, ArtifactStream artifactStream, T config);

  /**
   * Gets plans.
   *
   * @param config the jenkins config
   * @return the plans
   */
  Map<String, String> getPlans(T config);
}

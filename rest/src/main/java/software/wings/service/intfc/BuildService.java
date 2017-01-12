package software.wings.service.intfc;

import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by peeyushaggarwal on 5/13/16.
 */
public interface BuildService<T> {
  /**
   * Gets builds.
   *
   * @param appId            the app id
   * @param artifactSourceId the artifact source id
   * @param config    the jenkins config
   * @return the builds
   */
  List<BuildDetails> getBuilds(String appId, String artifactSourceId, T config);

  /**
   * Gets jobs.
   *
   * @param jenkinsConfig the jenkins setting id
   * @return the jobs
   */
  Set<String> getJobs(T jenkinsConfig);

  /**
   * Gets artifact paths.
   *
   * @param jobName       the job name
   * @param config the jenkins config
   * @return the artifact paths
   */
  Set<String> getArtifactPaths(String jobName, T config);

  /**
   * Gets last successful build.
   *
   * @param appId            the app id
   * @param artifactStreamId the artifact stream id
   * @param config    the jenkins config
   * @return the last successful build
   */
  BuildDetails getLastSuccessfulBuild(String appId, String artifactStreamId, T config);

  /**
   * Gets plans.
   *
   * @param config the jenkins config
   * @return the plans
   */
  Map<String, String> getPlans(T config);
}

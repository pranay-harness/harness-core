package software.wings.service.intfc;

import software.wings.beans.JenkinsConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;
import java.util.Set;

/**
 * Created by peeyushaggarwal on 5/13/16.
 */
public interface JenkinsBuildService {
  /**
   * Gets builds.
   *
   * @param appId            the app id
   * @param artifactSourceId the artifact source id
   * @param jenkinsConfig    the jenkins config
   * @return the builds
   */
  List<BuildDetails> getBuilds(String appId, String artifactSourceId, JenkinsConfig jenkinsConfig);

  /**
   * Gets jobs.
   *
   * @param jenkinsConfig the jenkins setting id
   * @return the jobs
   */
  Set<String> getJobs(JenkinsConfig jenkinsConfig);

  /**
   * Gets artifact paths.
   *
   * @param jobName       the job name
   * @param jenkinsConfig the jenkins config
   * @return the artifact paths
   */
  Set<String> getArtifactPaths(String jobName, JenkinsConfig jenkinsConfig);

  /**
   * Gets last successful build.
   *
   * @param appId            the app id
   * @param artifactStreamId the artifact stream id
   * @param jenkinsConfig    the jenkins config
   * @return the last successful build
   */
  BuildDetails getLastSuccessfulBuild(String appId, String artifactStreamId, JenkinsConfig jenkinsConfig);
}

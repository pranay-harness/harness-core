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
   * @param artifactSourceId the artifact source id
   * @param appId            the app id
   * @param jenkinsConfig    the jenkins config
   * @return the builds
   */
  List<BuildDetails> getBuilds(String artifactSourceId, String appId, JenkinsConfig jenkinsConfig);

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
}

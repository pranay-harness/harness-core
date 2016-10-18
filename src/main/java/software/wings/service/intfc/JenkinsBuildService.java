package software.wings.service.intfc;

import software.wings.beans.artifact.JenkinsConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by peeyushaggarwal on 5/13/16.
 */
public interface JenkinsBuildService {
  /**
   * Gets the builds.
   *
   * @param queryParameters the query parameters
   * @param jenkinsConfig   the jenkins config
   * @return the builds
   * @throws IOException Signals that an I/O exception has occurred.
   */
  List<BuildDetails> getBuilds(MultivaluedMap<String, String> queryParameters, JenkinsConfig jenkinsConfig)
      throws IOException;

  /**
   * Gets builds.
   *
   * @param appId              the app id
   * @param releaseId          the release id
   * @param artifactSourceName the artifact source name
   * @param jenkinsConfig      the jenkins config
   * @return the builds
   */
  List<BuildDetails> getBuilds(String appId, String releaseId, String artifactSourceName, JenkinsConfig jenkinsConfig);

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

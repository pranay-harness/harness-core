package software.wings.service.intfc;

import software.wings.beans.BambooConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;
import java.util.Set;

/**
 * Created by anubhaw on 11/22/16.
 */
public interface BambooBuildService {
  /**
   * Gets builds.
   *
   * @param appId            the app id
   * @param artifactSourceId the artifact source id
   * @param bambooConfig     the bamboo config
   * @return the builds
   */
  List<BuildDetails> getBuilds(String appId, String artifactSourceId, BambooConfig bambooConfig);

  /**
   * Gets jobs.
   *
   * @param bambooConfig the bamboo config
   * @return the jobs
   */
  Set<String> getJobs(BambooConfig bambooConfig);

  /**
   * Gets artifact paths.
   *
   * @param jobName      the job name
   * @param bambooConfig the bamboo config
   * @return the artifact paths
   */
  Set<String> getArtifactPaths(String jobName, BambooConfig bambooConfig);

  /**
   * Gets last successful build.
   *
   * @param appId            the app id
   * @param artifactStreamId the artifact stream id
   * @param bambooConfig     the bamboo config
   * @return the last successful build
   */
  BuildDetails getLastSuccessfulBuild(String appId, String artifactStreamId, BambooConfig bambooConfig);
}

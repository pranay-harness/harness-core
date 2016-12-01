package software.wings.helpers.ext.bamboo;

import software.wings.beans.BambooConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.io.InputStream;
import java.util.List;

/**
 * Created by anubhaw on 11/29/16.
 */
public interface BambooService {
  /**
   * Gets job keys.
   *
   * @param bambooConfig the bamboo config
   * @return the job keys
   */
  List<String> getJobKeys(BambooConfig bambooConfig);

  /**
   * Gets last successful build.
   *
   * @param bambooConfig the bamboo config
   * @param jobname      the jobname
   * @return the last successful build
   */
  BuildDetails getLastSuccessfulBuild(BambooConfig bambooConfig, String jobname);

  /**
   * Gets builds for job.
   *
   * @param bambooConfig      the bamboo config
   * @param jobname           the jobname
   * @param maxNumberOfBuilds the max number of builds
   * @return the builds for job
   */
  List<BuildDetails> getBuildsForJob(BambooConfig bambooConfig, String jobname, int maxNumberOfBuilds);

  /**
   * Gets artifact path.
   *
   * @param bambooConfig the bamboo config
   * @param jobName      the job name
   * @return the artifact path
   */
  List<String> getArtifactPath(BambooConfig bambooConfig, String jobName);

  /**
   * Download artifact pair.
   *
   *
   * @param bambooConfig
   * @param jobname           the jobname
   * @param buildNumber       the build number
   * @param artifactPathRegex the artifact path regex
   * @return the pair
   */
  org.apache.commons.lang3.tuple.Pair<String, InputStream> downloadArtifact(
      BambooConfig bambooConfig, String jobname, String buildNumber, String artifactPathRegex);
}

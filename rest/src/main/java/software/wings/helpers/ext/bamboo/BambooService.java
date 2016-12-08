package software.wings.helpers.ext.bamboo;

import software.wings.beans.BambooConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 11/29/16.
 */
public interface BambooService {
  /**
   * Gets job keys.
   *
   * @param bambooConfig the bamboo config
   * @param planKey
   * @return the job keys
   */
  List<String> getJobKeys(BambooConfig bambooConfig, String planKey);

  /**
   * Gets plan keys.
   *
   * @param bambooConfig the bamboo config
   * @return the plan keys
   */
  Map<String, String> getPlanKeys(BambooConfig bambooConfig);

  /**
   * Gets last successful build.
   *
   * @param bambooConfig the bamboo config
   * @param planKey      the jobname
   * @return the last successful build
   */
  BuildDetails getLastSuccessfulBuild(BambooConfig bambooConfig, String planKey);

  /**
   * Gets builds for job.
   *
   * @param bambooConfig      the bamboo config
   * @param planKey           the jobname
   * @param maxNumberOfBuilds the max number of builds
   * @return the builds for job
   */
  List<BuildDetails> getBuildsForJob(BambooConfig bambooConfig, String planKey, int maxNumberOfBuilds);

  /**
   * Gets artifact path.
   *
   * @param bambooConfig the bamboo config
   * @param planKey      the job name
   * @return the artifact path
   */
  List<String> getArtifactPath(BambooConfig bambooConfig, String planKey);

  /**
   * Download artifact pair.
   *
   * @param bambooConfig      the bamboo config
   * @param planKey           the jobname
   * @param buildNumber       the build number
   * @param artifactPathRegex the artifact path regex
   * @return the pair
   */
  org.apache.commons.lang3.tuple.Pair<String, InputStream> downloadArtifact(
      BambooConfig bambooConfig, String planKey, String buildNumber, String artifactPathRegex);
}

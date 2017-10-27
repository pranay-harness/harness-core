package software.wings.helpers.ext.bamboo;

import software.wings.beans.BambooConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;

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
   * @param planKey      the plan key
   * @return the job keys
   */
  List<String> getJobKeys(BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails, String planKey);

  /**
   * Gets plan keys.
   *
   * @param bambooConfig the bamboo config
   * @return the plan keys
   */
  Map<String, String> getPlanKeys(BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails);

  /**
   * Gets last successful build.
   *
   * @param bambooConfig the bamboo config
   * @param planKey      the jobname
   * @return the last successful build
   */
  BuildDetails getLastSuccessfulBuild(
      BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails, String planKey);

  /**
   * Gets builds for job.
   *
   * @param bambooConfig      the bamboo config
   * @param planKey           the jobname
   * @param maxNumberOfBuilds the max number of builds
   * @return the builds for job
   */
  List<BuildDetails> getBuilds(
      BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails, String planKey, int maxNumberOfBuilds);

  /**
   * Gets artifact path.
   *
   * @param bambooConfig the bamboo config
   * @param planKey      the job name
   * @return the artifact path
   */
  List<String> getArtifactPath(BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails, String planKey);

  /**
   * Download artifact pair.
   *
   * @param bambooConfig      the bamboo config
   * @param planKey           the jobname
   * @param buildNumber       the build number
   * @param artifactPathRegex the artifact path regex
   * @return the pair
   */
  org.apache.commons.lang3.tuple.Pair<String, InputStream> downloadArtifact(BambooConfig bambooConfig,
      List<EncryptedDataDetail> encryptionDetails, String planKey, String buildNumber, String artifactPathRegex);

  /**
   * Is running boolean.
   *
   * @param bambooConfig the bamboo config
   * @return the boolean
   */
  boolean isRunning(BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails);

  /**
   * Triggers Project Plan
   *
   * @param planKey    the plankey
   * @param parameters the parameters
   * @return Build Result Key {projectKey}-{buildKey}-{buildNumber}
   */
  String triggerPlan(BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails, String planKey,
      Map<String, String> parameters);

  /**
   * Retrieves the bamboo build result
   *
   * @param bambooConfig   BambooConfig
   * @param buildResultKey Build result key {projectKey}-{buildKey}-{buildNumber}
   * @return
   */
  Result getBuildResult(BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails, String buildResultKey);

  /**
   * Retrieves the bamboo build running result status
   *
   * @param bambooConfig   BambooConfig
   * @param buildResultKey Build result key {projectKey}-{buildKey}-{buildNumber}
   * @return
   */
  Status getBuildResultStatus(
      BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails, String buildResultKey);
}

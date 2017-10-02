package software.wings.helpers.ext.bamboo;

import software.wings.beans.BambooConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.waitnotify.ListNotifyResponseData;

import java.io.IOException;
import java.net.URISyntaxException;
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
  List<BuildDetails> getBuilds(BambooConfig bambooConfig, String planKey, int maxNumberOfBuilds);

  /**
   * Gets artifact path.
   *
   * @param bambooConfig the bamboo config
   * @param planKey      the job name
   * @return the artifact path
   */
  List<String> getArtifactPath(BambooConfig bambooConfig, String planKey);

  /**
   * Downloads the artifacts from bamboo server
   * @param bambooConfig    bamboo config
   * @param planKey         plan key
   * @param buildNumber     build number
   * @param artifactPaths   artifact paths from the artifact source
   * @param delegateId      delegate id
   * @param taskId          task id
   * @param accountId       account id
   * @return ListNotifyResponseData
   * @throws IOException
   * @throws URISyntaxException
   */
  ListNotifyResponseData downloadArtifacts(BambooConfig bambooConfig, String planKey, String buildNumber,
      List<String> artifactPaths, String delegateId, String taskId, String accountId)
      throws IOException, URISyntaxException;

  /**
   * Is running boolean.
   *
   * @param bambooConfig the bamboo config
   * @return the boolean
   */
  boolean isRunning(BambooConfig bambooConfig);

  /**
   * Triggers Project Plan
   *
   * @param planKey    the plankey
   * @param parameters the parameters
   * @return Build Result Key {projectKey}-{buildKey}-{buildNumber}
   */
  String triggerPlan(BambooConfig bambooConfig, String planKey, Map<String, String> parameters);

  /**
   * Retrieves the bamboo build result
   * @param bambooConfig BambooConfig
   * @param buildResultKey Build result key {projectKey}-{buildKey}-{buildNumber}
   * @return
   */
  Result getBuildResult(BambooConfig bambooConfig, String buildResultKey);

  /**
   * Retrieves the bamboo build running result status
   * @param bambooConfig BambooConfig
   * @param buildResultKey  Build result key {projectKey}-{buildKey}-{buildNumber}
   * @return
   */
  Status getBuildResultStatus(BambooConfig bambooConfig, String buildResultKey);
}

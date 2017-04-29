package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by anubhaw on 8/18/16.
 */
public interface BuildSourceService {
  /**
   * Gets jobs.
   *
   * @param settingId the jenkins setting id
   * @return the jobs
   */
  Set<String> getJobs(@NotEmpty String appId, @NotEmpty String settingId);

  /**
   * Gets plans.
   *
   * @param settingId the setting id
   * @return the plans
   */
  Map<String, String> getPlans(@NotEmpty String appId, @NotEmpty String settingId);

  /**
   * Gets artifact paths.
   *
   * @param jobName   the job name
   * @param settingId the setting id
   * @param groupId the group id
   * @return the artifact paths
   */
  Set<String> getArtifactPaths(
      @NotEmpty String appId, @NotEmpty String jobName, @NotEmpty String settingId, String groupId);

  /**
   * Gets builds.
   *
   * @param appId            the app id
   * @param artifactStreamId the artifact source id
   * @param settingId        the setting id
   * @return the builds
   */
  List<BuildDetails> getBuilds(String appId, String artifactStreamId, String settingId);

  /**
   * Gets last successful build.
   *
   * @param appId            the app id
   * @param artifactStreamId the artifact stream id
   * @param settingId        the setting id
   * @return the last successful build
   */
  BuildDetails getLastSuccessfulBuild(String appId, String artifactStreamId, String settingId);

  /**
   * Gets group Id paths.
   *
   * @param jobName   the job name
   * @param settingId the setting id
   * @return the groupId paths
   */
  Set<String> getGroupIds(@NotEmpty String appId, @NotEmpty String jobName, @NotEmpty String settingId);

  /**
   * Valiate Artifact Stream
   * @param appId
   * @param settingId
   * @param artifactStreamAttributes
   * @throws software.wings.exception.WingsException if Artifact Stream not valid
   */
  boolean validateArtifactSource(
      @NotEmpty String appId, @NotEmpty String settingId, ArtifactStreamAttributes artifactStreamAttributes);
}

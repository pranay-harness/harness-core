package software.wings.service.intfc;

import io.harness.exception.WingsException;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Created by anubhaw on 8/18/16.
 */
public interface BuildSourceService {
  /**
   * Gets jobs.
   *
   * @param appId     the app id
   * @param settingId the jenkins setting id
   * @param parentJobName the jenkins parent job name (if any)
   * @return the jobs
   */
  Set<JobDetails> getJobs(@NotEmpty String appId, @NotEmpty String settingId, @Nullable String parentJobName);

  /**
   * Gets plans.
   *
   * @param appId     the app id
   * @param settingId the setting id
   * @param artifactStreamType artifact stream type
   * @return the plans
   */
  Map<String, String> getPlans(@NotEmpty String appId, @NotEmpty String settingId, String artifactStreamType);

  /**
   * Get project.
   *
   * @param appId     the app id
   * @param settingId the setting id
   * @return the project (now GCS only)
   */
  String getProject(String appId, String settingId);

  /**
   * Get buckets.
   *
   * @param appId     the app id
   * @param projectId the project id
   * @param settingId the setting id
   * @return the project (now GCS only)
   */
  Map<String, String> getBuckets(String appId, String projectId, String settingId);

  /**
   * Get SMB paths.
   *
   * @param appId     the app id
   * @param settingId the setting id
   * @return the paths (now SMB only)
   */
  List<String> getSmbPaths(String appId, String settingId);

  /**
   * Gets plans.
   *
   * @param appId     the app id
   * @param settingId the setting id
   * @param artifactStreamType artifact stream type
   * @return the plans
   */
  Map<String, String> getPlans(@NotEmpty String appId, @NotEmpty String settingId, @NotEmpty String serviceId,
      String artifactStreamType, String repositoryType);

  /**
   * Gets artifact paths.
   *
   * @param appId     the app id
   * @param jobName   the job name
   * @param settingId the setting id
   * @param groupId   the group id
   * @param artifactStreamType artifact stream type
   * @return the artifact paths
   */
  Set<String> getArtifactPaths(@NotEmpty String appId, @NotEmpty String jobName, @NotEmpty String settingId,
      String groupId, String artifactStreamType);

  /**
   * Gets builds.
   *
   * @param appId            the app id
   * @param artifactStreamId the artifact source id
   * @param settingId        the setting id
   * @return the builds
   */
  List<BuildDetails> getBuilds(String appId, String artifactStreamId, String settingId);

  /***
   * Gets builds with the limit
   * @param appId
   * @param artifactStreamId
   * @param settingId
   * @param limit
   * @return
   */
  List<BuildDetails> getBuilds(String appId, String artifactStreamId, String settingId, int limit);

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
   * @param appId     the app id
   * @param jobName   the job name
   * @param settingId the setting id
   * @return the groupId paths
   */
  Set<String> getGroupIds(@NotEmpty String appId, @NotEmpty String jobName, @NotEmpty String settingId);

  /**
   * Valiate Artifact Stream
   *
   * @param appId                    the app id
   * @param settingId                the setting id
   * @param artifactStreamAttributes the artifact stream attributes
   * @return the boolean
   * @throws WingsException if Artifact Stream not valid
   */
  boolean validateArtifactSource(
      @NotEmpty String appId, @NotEmpty String settingId, ArtifactStreamAttributes artifactStreamAttributes);

  /**
   * Get Job details
   * @param appId
   * @param settingId
   * @param jobName
   * @return
   */
  JobDetails getJob(@NotEmpty String appId, @NotEmpty String settingId, @NotEmpty String jobName);

  /**
   * Gets build service.
   *
   * @param settingAttribute the setting attribute
   * @param appId            the app id
   * @return the build service
   */
  BuildService getBuildService(SettingAttribute settingAttribute, String appId);

  /***
   * Collects an artifact
   * @param appId
   * @param artifactStreamId
   * @param buildDetails
   * @return
   */
  Artifact collectArtifact(String appId, String artifactStreamId, BuildDetails buildDetails);
}

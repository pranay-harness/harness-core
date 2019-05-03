package software.wings.service.intfc;

import io.harness.exception.WingsException;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.utils.RepositoryType;

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
  default Set<JobDetails> getJobs(@NotEmpty String appId, @NotEmpty String settingId, @Nullable String parentJobName) {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets plans.
   *
   * @param appId     the app id
   * @param settingId the setting id
   * @param artifactStreamType artifact stream type
   * @return the plans
   */
  default Map<String, String> getPlans(@NotEmpty String appId, @NotEmpty String settingId, String artifactStreamType) {
    throw new UnsupportedOperationException();
  }

  /**
   * Get project.
   *
   * @param appId     the app id
   * @param settingId the setting id
   * @return the project (now GCS only)
   */
  default String getProject(String appId, String settingId) {
    throw new UnsupportedOperationException();
  }

  /**
   * Get buckets.
   *
   * @param appId     the app id
   * @param projectId the project id
   * @param settingId the setting id
   * @return the project (now GCS only)
   */
  default Map<String, String> getBuckets(String appId, String projectId, String settingId) {
    throw new UnsupportedOperationException();
  }

  /**
   * Get SMB paths.
   *
   * @param appId     the app id
   * @param settingId the setting id
   * @return the paths (now SMB only)
   */
  default List<String> getSmbPaths(String appId, String settingId) {
    throw new UnsupportedOperationException();
  }
  /**
   * Get Artifact paths by stream id.
   *
   * @param appId     the app id
   * @param settingId the setting id
   * @param streamType artifact stream type
   * @return the paths
   */
  default List<String> getArtifactPathsByStreamType(String appId, String settingId, String streamType) {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets plans.
   *
   * @param appId     the app id
   * @param settingId the setting id
   * @param artifactStreamType artifact stream type
   * @return the plans
   */
  default Map<String, String> getPlans(@NotEmpty String appId, @NotEmpty String settingId, @NotEmpty String serviceId,
      String artifactStreamType, String repositoryType) {
    throw new UnsupportedOperationException();
  }

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
  default Set<String> getArtifactPaths(@NotEmpty String appId, @NotEmpty String jobName, @NotEmpty String settingId,
      String groupId, String artifactStreamType) {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets builds.
   *
   * @param appId            the app id
   * @param artifactStreamId the artifact source id
   * @param settingId        the setting id
   * @return the builds
   */
  default List<BuildDetails> getBuilds(String appId, String artifactStreamId, String settingId) {
    throw new UnsupportedOperationException();
  }

  /***
   * Gets builds with the limit
   * @param appId
   * @param artifactStreamId
   * @param settingId
   * @param limit
   * @return
   */
  default List<BuildDetails> getBuilds(String appId, String artifactStreamId, String settingId, int limit) {
    throw new UnsupportedOperationException();
  }

  default List<BuildDetails> getBuilds(String artifactStreamId, String settingId, int limit) {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets last successful build.
   *
   * @param appId            the app id
   * @param artifactStreamId the artifact stream id
   * @param settingId        the setting id
   * @return the last successful build
   */
  default BuildDetails getLastSuccessfulBuild(String appId, String artifactStreamId, String settingId) {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets group Id paths.
   *
   * @param appId     the app id
   * @param jobName   the job name
   * @param settingId the setting id
   * @return the groupId paths
   */
  default Set<String> getGroupIds(@NotEmpty String appId, @NotEmpty String jobName, @NotEmpty String settingId) {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets group Id paths.
   *
   * @param jobName   the job name
   * @param settingId the setting id
   * @return the groupId paths
   */
  default Set<String> getGroupIds(@NotEmpty String jobName, @NotEmpty String settingId) {
    throw new UnsupportedOperationException();
  }

  /**
   * Validate Artifact Stream
   *
   * @param appId                    the app id
   * @param settingId                the setting id
   * @param artifactStreamAttributes the artifact stream attributes
   * @return the boolean
   * @throws WingsException if Artifact Stream not valid
   */
  default boolean validateArtifactSource(
      @NotEmpty String appId, @NotEmpty String settingId, ArtifactStreamAttributes artifactStreamAttributes) {
    throw new UnsupportedOperationException();
  }

  /**
   * Validate Artifact Stream
   * @param artifactStream
   * @return
   */
  default boolean validateArtifactSource(ArtifactStream artifactStream) {
    throw new UnsupportedOperationException();
  }

  /**
   * Get Job details
   * @param appId
   * @param settingId
   * @param jobName
   * @return
   */
  default JobDetails getJob(@NotEmpty String appId, @NotEmpty String settingId, @NotEmpty String jobName) {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets build service.
   *
   * @param settingAttribute the setting attribute
   * @param appId            the app id
   * @return the build service
   */
  default BuildService getBuildService(SettingAttribute settingAttribute, String appId) {
    throw new UnsupportedOperationException();
  }

  /***
   * Collects an artifact
   * @param appId
   * @param artifactStreamId
   * @param buildDetails
   * @return
   */
  default Artifact collectArtifact(String appId, String artifactStreamId, BuildDetails buildDetails) {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets jobs.
   *
   * @param settingId the jenkins setting id
   * @param parentJobName the jenkins parent job name (if any)
   * @return the jobs
   */
  default Set<JobDetails> getJobs(@NotEmpty String settingId, @Nullable String parentJobName) {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets build service.
   *
   * @param settingAttribute the setting attribute
   * @return the build service
   */
  default BuildService getBuildService(SettingAttribute settingAttribute) {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets artifact paths.
   *
   * @param jobName   the job name
   * @param settingId the setting id
   * @param groupId   the group id
   * @param artifactStreamType artifact stream type
   * @return the artifact paths
   */
  default Set<String> getArtifactPaths(
      @NotEmpty String jobName, @NotEmpty String settingId, String groupId, String artifactStreamType) {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets last successful build.
   *
   * @param artifactStreamId the artifact stream id
   * @param settingId        the setting id
   * @return the last successful build
   */
  default BuildDetails getLastSuccessfulBuild(String artifactStreamId, String settingId) {
    throw new UnsupportedOperationException();
  }

  default Artifact collectArtifact(String artifactStreamId, BuildDetails buildDetails) {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets plans.
   *
   * @param settingId the setting id
   * @param artifactStreamType artifact stream type
   * @return the plans
   */
  default Map<String, String> getPlans(@NotEmpty String settingId, String artifactStreamType) {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets plans for repository type.
   *
   * @param settingId
   * @param streamType
   * @param repositoryType
   * @return
   */
  default Map<String, String> getPlansForRepositoryType(
      @NotEmpty String settingId, String streamType, RepositoryType repositoryType) {
    throw new UnsupportedOperationException();
  }

  /**
   * Get project.
   *
   * @param settingId the setting id
   * @return the project (now GCS only)
   */
  default String getProject(String settingId) {
    throw new UnsupportedOperationException();
  }

  /**
   * Get buckets.
   *
   * @param projectId the project id
   * @param settingId the setting id
   * @return the project (now GCS only)
   */
  default Map<String, String> getBuckets(String projectId, String settingId) {
    throw new UnsupportedOperationException();
  }

  /**
   * Get SMB paths.
   *
   * @param settingId the setting id
   * @return the paths (now SMB only)
   */
  default List<String> getSmbPaths(String settingId) {
    throw new UnsupportedOperationException();
  }

  /**
   * Get Artifact paths by stream id.
   *
   * @param settingId the setting id
   * @param streamType artifact stream type
   * @return the paths
   */
  default List<String> getArtifactPathsByStreamType(String settingId, String streamType) {
    throw new UnsupportedOperationException();
  }
}

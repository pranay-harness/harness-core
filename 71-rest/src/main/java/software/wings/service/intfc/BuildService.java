package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.collect.Lists;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.settings.azureartifacts.AzureArtifactsConfig;
import software.wings.helpers.ext.azure.devops.AzureArtifactsFeed;
import software.wings.helpers.ext.azure.devops.AzureArtifactsPackage;
import software.wings.helpers.ext.azure.devops.AzureDevopsProject;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.utils.ArtifactType;
import software.wings.utils.RepositoryFormat;
import software.wings.utils.RepositoryType;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by peeyushaggarwal on 5/13/16.
 *
 * @param <T> the type parameter
 */
@OwnedBy(CDC)
public interface BuildService<T> {
  /**
   * Gets builds.
   *
   * @param appId                    the app id
   * @param artifactStreamAttributes the build service request params
   * @param config                   the jenkins config
   * @return the builds
   */
  default List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes, T config,
      List<EncryptedDataDetail> encryptionDetails) {
    throw new UnsupportedOperationException();
  }

  /***
   * Gets specified limited builds
   * @param appId
   * @param artifactStreamAttributes
   * @param config
   * @param encryptionDetails
   * @param limit
   * @return
   */
  default List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes, T config,
      List<EncryptedDataDetail> encryptionDetails, int limit) {
    return getBuilds(appId, artifactStreamAttributes, config, encryptionDetails);
  }

  default List<BuildDetails> getBuilds(ArtifactStreamAttributes artifactStreamAttributes) {
    throw new UnsupportedOperationException("Supported only for Custom Artifact Source");
  }

  default BuildDetails getBuild(String appId, ArtifactStreamAttributes artifactStreamAttributes, T config,
      List<EncryptedDataDetail> encryptionDetails, String buildNo) {
    throw new UnsupportedOperationException();
  }

  default boolean validateArtifactSource(ArtifactStreamAttributes artifactStreamAttributes) {
    throw new UnsupportedOperationException("Supported only for Custom Artifact Source");
  }

  /**
   * Gets jobs.
   *
   * @param jenkinsConfig the jenkins setting id
   * @param parentJobName parent job name if any
   * @return the jobs
   */
  default List<JobDetails> getJobs(
      T jenkinsConfig, List<EncryptedDataDetail> encryptionDetails, Optional<String> parentJobName) {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets artifact paths.
   *
   * @param jobName the job name
   * @param groupId the Group Id
   * @param config  the jenkins config
   * @return the artifact paths
   */
  default List<String> getArtifactPaths(
      String jobName, String groupId, T config, List<EncryptedDataDetail> encryptionDetails) {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets artifact paths.
   *
   * @param jobName the job name
   * @param groupId the Group Id
   * @param config  the nexus config
   * @param config  the repositoryType
   * @return the artifact paths
   */
  default List<String> getArtifactPaths(
      String jobName, String groupId, T config, List<EncryptedDataDetail> encryptionDetails, String repositoryType) {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets artifact paths using nexus3 private apis.
   *
   * @param jobName the job name
   * @param groupId the Group Id
   * @param config  the nexus config
   * @param config  the repositoryType
   * @return the artifact paths
   */
  default List<String> getArtifactPathsUsingPrivateApis(
      String jobName, String groupId, T config, List<EncryptedDataDetail> encryptionDetails, String repositoryType) {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets last successful build.
   *
   * @param appId                    the app id
   * @param artifactStreamAttributes the build service request params
   * @param config                   the jenkins config
   * @return the last successful build
   */
  default BuildDetails getLastSuccessfulBuild(String appId, ArtifactStreamAttributes artifactStreamAttributes, T config,
      List<EncryptedDataDetail> encryptionDetails) {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets plans.
   *
   * @param config the  config
   * @return the plans
   */
  default Map<String, String> getPlans(T config, List<EncryptedDataDetail> encryptionDetails) {
    throw new UnsupportedOperationException();
  }

  /**
   * Get buckets
   *
   * @param config the  config
   * @param projectId GCS project id
   * @return the buckets
   */
  default Map<String, String> getBuckets(T config, String projectId, List<EncryptedDataDetail> encryptionDetails) {
    throw new UnsupportedOperationException();
  }

  /**
   * Get SMB paths
   *
   * @param config the  config
   * @return the smb paths
   */
  default List<String> getSmbPaths(T config, List<EncryptedDataDetail> encryptionDetails) {
    throw new UnsupportedOperationException();
  }
  /**
   * Get SFTP paths
   *
   * @param config the  config
   * @return the sftp paths
   */
  default List<String> getArtifactPathsByStreamType(
      T config, List<EncryptedDataDetail> encryptionDetails, String streamType) {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets plans.
   *
   * @param config the  config
   * @param config
   * @return the plans
   */
  default Map<String, String> getPlans(
      T config, List<EncryptedDataDetail> encryptionDetails, ArtifactType artifactType, String repositoryType) {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets plans.
   *
   * @param config the  config
   * @param config
   * @return the plans
   */
  default Map<String, String> getPlans(
      T config, List<EncryptedDataDetail> encryptionDetails, RepositoryFormat repositoryFormat) {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets plans.
   *
   * @param config the  config
   * @param config
   * @return the plans
   */
  default Map<String, String> getPlans(
      T config, List<EncryptedDataDetail> encryptionDetails, RepositoryType repositoryType) {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets group Id paths.
   *
   * @param repositoryName The repo id or name
   * @param config   the config
   * @return the groupId paths
   */
  default List<String> getGroupIds(String repositoryName, T config, List<EncryptedDataDetail> encryptionDetails) {
    throw new UnsupportedOperationException();
  }

  default List<String> getGroupIds(
      String repositoryName, String repositoryType, T config, List<EncryptedDataDetail> encryptionDetails) {
    throw new UnsupportedOperationException();
  }

  default List<String> getGroupIdsUsingPrivateApis(
      String repositoryName, String repositoryType, T config, List<EncryptedDataDetail> encryptionDetails) {
    throw new UnsupportedOperationException();
  }

  /**
   * Validates Artifact Server
   *
   * @param config
   */
  default boolean validateArtifactServer(T config, List<EncryptedDataDetail> encryptedDataDetails) {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets the all the Job details
   * @param jobName
   * @return
   */
  default JobDetails getJob(String jobName, T config, List<EncryptedDataDetail> encryptionDetails) {
    throw new UnsupportedOperationException();
  }

  /**
   * Validates Artifact Stream
   *
   * @param artifactStreamAttributes
   */
  default boolean validateArtifactSource(
      T config, List<EncryptedDataDetail> encryptionDetails, ArtifactStreamAttributes artifactStreamAttributes) {
    throw new UnsupportedOperationException();
  }

  default List<JobDetails> wrapJobNameWithJobDetails(Collection<String> jobNames) {
    List<JobDetails> jobDetailsList = Lists.newArrayListWithExpectedSize(jobNames.size());
    for (String jobName : jobNames) {
      JobDetails jobDetails = new JobDetails(jobName, false);
      jobDetailsList.add(jobDetails);
    }
    return jobDetailsList;
  }

  default List<String> extractJobNameFromJobDetails(List<JobDetails> jobs) {
    List<String> jobNames = Lists.newArrayListWithExpectedSize(jobs.size());
    for (JobDetails job : jobs) {
      jobNames.add(job.getJobName());
    }
    return jobNames;
  }

  default List<Map<String, String>> getLabels(ArtifactStreamAttributes artifactStreamAttributes, List<String> buildNos,
      T dockerConfig, List<EncryptedDataDetail> encryptionDetails) {
    throw new UnsupportedOperationException();
  }

  default List<AzureDevopsProject> getProjects(
      AzureArtifactsConfig azureArtifactsConfig, List<EncryptedDataDetail> encryptionDetails) {
    throw new UnsupportedOperationException();
  }

  default List<AzureArtifactsFeed> getFeeds(
      AzureArtifactsConfig azureArtifactsConfig, List<EncryptedDataDetail> encryptionDetails, String project) {
    throw new UnsupportedOperationException();
  }

  default List<AzureArtifactsPackage> getPackages(AzureArtifactsConfig azureArtifactsConfig,
      List<EncryptedDataDetail> encryptionDetails, String project, String feed, String protocolType) {
    throw new UnsupportedOperationException();
  }

  /**
   * wrapNewBuildsWithLabels removes build details already present in DB and collects labels if necessary.
   */
  default List<BuildDetails> wrapNewBuildsWithLabels(
      List<BuildDetails> buildDetails, ArtifactStreamAttributes artifactStreamAttributes, T config) {
    // NOTE: config and encryptionDetails are used only for fetching labels.
    // Filter out new build details that are not saved already in our DB.
    buildDetails = ArtifactCollectionUtils.getNewBuildDetails(artifactStreamAttributes.getSavedBuildDetailsKeys(),
        buildDetails, artifactStreamAttributes.getArtifactStreamType(), artifactStreamAttributes);

    // Disable fetching labels for now as it has many issues.
    return buildDetails;
  }

  /**
   * wrapLastSuccessfulBuildWithLabels removes build details already present in DB and collects labels if necessary.
   */
  default BuildDetails wrapLastSuccessfulBuildWithLabels(
      BuildDetails buildDetails, ArtifactStreamAttributes artifactStreamAttributes, T config) {
    if (buildDetails == null) {
      return null;
    }
    List<BuildDetails> buildDetailsList =
        wrapNewBuildsWithLabels(Collections.singletonList(buildDetails), artifactStreamAttributes, config);
    return isEmpty(buildDetailsList) ? null : buildDetailsList.get(0);
  }
}

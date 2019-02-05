package software.wings.service.intfc;

import com.google.common.collect.Lists;

import io.harness.exception.WingsException;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.ArtifactType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by peeyushaggarwal on 5/13/16.
 *
 * @param <T> the type parameter
 */
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
   * Gets group Id paths.
   *
   * @param repoType The repo type
   * @param config   the config
   * @return the groupId paths
   */
  default List<String> getGroupIds(String repoType, T config, List<EncryptedDataDetail> encryptionDetails) {
    throw new UnsupportedOperationException();
  }

  /**
   * Validates Artifact Server
   *
   * @param config
   * @throws WingsException if not valid
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
   * @throws WingsException if not valid
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
}

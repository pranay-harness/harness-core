package software.wings.service.intfc;

import com.google.common.collect.Lists;

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
  List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes, T config,
      List<EncryptedDataDetail> encryptionDetails);

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

  /**
   * Gets jobs.
   *
   * @param jenkinsConfig the jenkins setting id
   * @param parentJobName parent job name if any
   * @return the jobs
   */
  List<JobDetails> getJobs(
      T jenkinsConfig, List<EncryptedDataDetail> encryptionDetails, Optional<String> parentJobName);

  /**
   * Gets artifact paths.
   *
   * @param jobName the job name
   * @param groupId the Group Id
   * @param config  the jenkins config
   * @return the artifact paths
   */
  List<String> getArtifactPaths(String jobName, String groupId, T config, List<EncryptedDataDetail> encryptionDetails);

  /**
   * Gets last successful build.
   *
   * @param appId                    the app id
   * @param artifactStreamAttributes the build service request params
   * @param config                   the jenkins config
   * @return the last successful build
   */
  BuildDetails getLastSuccessfulBuild(String appId, ArtifactStreamAttributes artifactStreamAttributes, T config,
      List<EncryptedDataDetail> encryptionDetails);

  /**
   * Gets plans.
   *
   * @param config the  config
   * @return the plans
   */
  Map<String, String> getPlans(T config, List<EncryptedDataDetail> encryptionDetails);

  /**
   * Gets plans.
   *
   * @param config the  config
   * @param config
   * @return the plans
   */
  Map<String, String> getPlans(
      T config, List<EncryptedDataDetail> encryptionDetails, ArtifactType artifactType, String repositoryType);

  /**
   * Gets group Id paths.
   *
   * @param repoType The repo type
   * @param config   the config
   * @return the groupId paths
   */
  List<String> getGroupIds(String repoType, T config, List<EncryptedDataDetail> encryptionDetails);

  /**
   * Validates Artifact Server
   *
   * @param config
   * @throws software.wings.exception.WingsException if not valid
   */
  boolean validateArtifactServer(T config);

  /**
   * Gets the all the Job details
   * @param jobName
   * @return
   */
  default JobDetails getJob(String jobName, T config, List<EncryptedDataDetail> encryptionDetails) {
    return null;
  }
  /**
   * Validates Artifact Stream
   *
   * @param artifactStreamAttributes
   * @throws software.wings.exception.WingsException if not valid
   */
  boolean validateArtifactSource(
      T config, List<EncryptedDataDetail> encryptionDetails, ArtifactStreamAttributes artifactStreamAttributes);

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

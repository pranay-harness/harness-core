package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.INVALID_ARTIFACT_SERVER;
import static io.harness.exception.WingsException.SRE;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_ADMIN;
import static io.harness.network.Http.connectableHttpUrl;
import static java.util.stream.Collectors.toList;
import static software.wings.helpers.ext.jenkins.JobDetails.JobParameter;
import static software.wings.helpers.ext.jenkins.model.ParamPropertyType.BooleanParameterDefinition;
import static software.wings.utils.Validator.equalCheck;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.offbytwo.jenkins.model.Artifact;
import com.offbytwo.jenkins.model.JobWithDetails;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.common.Constants;
import software.wings.exception.InvalidRequestException;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.helpers.ext.jenkins.model.JobProperty;
import software.wings.helpers.ext.jenkins.model.JobWithExtendedDetails;
import software.wings.helpers.ext.jenkins.model.ParametersDefinitionProperty;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.jenkins.JenkinsUtil;
import software.wings.service.intfc.JenkinsBuildService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.ArtifactType;
import software.wings.utils.Misc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Created by peeyushaggarwal on 5/13/16.
 */
@Singleton
public class JenkinsBuildServiceImpl implements JenkinsBuildService {
  /**
   * The constant APP_ID.
   */
  public static final String APP_ID = "appId";

  /**
   * II
   * The constant ARTIFACT_STREAM_NAME.
   */
  private static final Logger logger = LoggerFactory.getLogger(JenkinsBuildServiceImpl.class);

  @Inject private EncryptionService encryptionService;
  @Inject private JenkinsUtil jenkinsUtil;

  @Override
  public List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      JenkinsConfig config, List<EncryptedDataDetail> encryptionDetails) {
    return getBuildDetails(artifactStreamAttributes, appId, config, encryptionDetails);
  }

  private List<BuildDetails> getBuildDetails(ArtifactStreamAttributes artifactStreamAttributes, String appId,
      JenkinsConfig jenkinsConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      equalCheck(artifactStreamAttributes.getArtifactStreamType(), ArtifactStreamType.JENKINS.name());

      encryptionService.decrypt(jenkinsConfig, encryptionDetails);
      Jenkins jenkins = jenkinsUtil.getJenkins(jenkinsConfig);
      return jenkins.getBuildsForJob(artifactStreamAttributes.getJobName(), 50);
    } catch (WingsException e) {
      throw e;
    } catch (IOException ex) {
      throw new InvalidRequestException(
          "Failed to fetch build details jenkins server. Reason:" + Misc.getMessage(ex), USER_ADMIN);
    }
  }

  @Override
  public List<JobDetails> getJobs(
      JenkinsConfig jenkinsConfig, List<EncryptedDataDetail> encryptionDetails, Optional<String> parentJobName) {
    try {
      encryptionService.decrypt(jenkinsConfig, encryptionDetails);
      Jenkins jenkins = jenkinsUtil.getJenkins(jenkinsConfig);
      // Just in case, some one passes null instead of Optional.empty()
      if (parentJobName == null) {
        return jenkins.getJobs(null);
      }
      return jenkins.getJobs(parentJobName.orElse(null));
    } catch (WingsException e) {
      throw e;
    } catch (IOException e) {
      throw new WingsException(INVALID_ARTIFACT_SERVER, USER)
          .addParam("message", "Failed to fetch Jobs. Reason:" + Misc.getMessage(e));
    }
  }

  @Override
  public List<String> getArtifactPaths(
      String jobName, String groupId, JenkinsConfig jenkinsConfig, List<EncryptedDataDetail> encryptionDetails) {
    encryptionService.decrypt(jenkinsConfig, encryptionDetails);
    Jenkins jenkins = jenkinsUtil.getJenkins(jenkinsConfig);
    try {
      JobWithDetails job = jenkins.getJob(jobName);
      return Lists.newArrayList(job.getLastSuccessfulBuild()
                                    .details()
                                    .getArtifacts()
                                    .parallelStream()
                                    .map(Artifact::getRelativePath)
                                    .distinct()
                                    .collect(toList()));
    } catch (WingsException e) {
      throw e;
    } catch (Exception ex) {
      throw new WingsException(INVALID_ARTIFACT_SERVER, USER)
          .addParam("message", "Error in artifact paths from jenkins server. Reason:" + Misc.getMessage(ex));
    }
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      JenkinsConfig jenkinsConfig, List<EncryptedDataDetail> encryptionDetails) {
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ArtifactStreamType.JENKINS.name());

    encryptionService.decrypt(jenkinsConfig, encryptionDetails);
    Jenkins jenkins = jenkinsUtil.getJenkins(jenkinsConfig);
    try {
      return jenkins.getLastSuccessfulBuildForJob(artifactStreamAttributes.getJobName());
    } catch (WingsException e) {
      throw e;
    } catch (IOException ex) {
      throw new WingsException(INVALID_ARTIFACT_SERVER, USER_ADMIN)
          .addParam("message", "Error in fetching build from jenkins server. Reason:" + Misc.getMessage(ex));
    }
  }

  @Override
  public Map<String, String> getPlans(JenkinsConfig jenkinsConfig, List<EncryptedDataDetail> encryptionDetails) {
    List<JobDetails> jobs = getJobs(jenkinsConfig, encryptionDetails, Optional.empty());
    Map<String, String> jobKeyMap = new HashMap<>();
    if (jobs != null) {
      jobs.forEach(jobKey -> jobKeyMap.put(jobKey.getJobName(), jobKey.getJobName()));
    }
    return jobKeyMap;
  }

  @Override
  public Map<String, String> getPlans(JenkinsConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactType artifactType, String repositoryType) {
    return getPlans(config, encryptionDetails);
  }

  @Override
  public List<String> getGroupIds(
      String jobName, JenkinsConfig jenkinsConfig, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by Jenkins Artifact Stream", SRE);
  }

  @Override
  public boolean validateArtifactServer(JenkinsConfig jenkinsConfig) {
    if (Constants.TOKEN_FIELD.equals(jenkinsConfig.getAuthMechanism())) {
      if (isEmpty(new String(jenkinsConfig.getToken()))) {
        throw new WingsException(INVALID_ARTIFACT_SERVER, USER).addParam("message", "Token should be not empty");
      }
    } else {
      if (isEmpty(jenkinsConfig.getUsername()) || isEmpty(new String(jenkinsConfig.getPassword()))) {
        throw new WingsException(INVALID_ARTIFACT_SERVER, USER)
            .addParam("message", "UserName/Password should be not empty");
      }
    }

    if (!connectableHttpUrl(jenkinsConfig.getJenkinsUrl())) {
      throw new WingsException(INVALID_ARTIFACT_SERVER, USER)
          .addParam("message", "Could not reach Jenkins Server at : " + jenkinsConfig.getJenkinsUrl());
    }
    encryptionService.decrypt(jenkinsConfig, Collections.emptyList());
    Jenkins jenkins = jenkinsUtil.getJenkins(jenkinsConfig);

    return jenkins.isRunning();
  }

  @Override
  public JobDetails getJob(String jobName, JenkinsConfig jenkinsConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      logger.info("Retrieving Job with details for Job: {}", jobName);
      encryptionService.decrypt(jenkinsConfig, encryptionDetails);
      Jenkins jenkins = jenkinsUtil.getJenkins(jenkinsConfig);
      JobWithDetails jobWithDetails = jenkins.getJob(jobName);
      List<JobParameter> parameters = new ArrayList<>();
      if (jobWithDetails != null) {
        JobWithExtendedDetails jobWithExtendedDetails = (JobWithExtendedDetails) jobWithDetails;
        List<JobProperty> properties = jobWithExtendedDetails.getProperties();
        if (properties != null) {
          properties.stream()
              .map(JobProperty::getParameterDefinitions)
              .filter(Objects::nonNull)
              .forEach((List<ParametersDefinitionProperty> pds) -> {
                logger.info("Job Properties definitions {}", pds.toArray());
                pds.forEach((ParametersDefinitionProperty pdProperty) -> parameters.add(getJobParameter(pdProperty)));
              });
        }
        logger.info("Retrieving Job with details for Job: {} success", jobName);
        return new JobDetails(jobWithDetails.getName(), jobWithDetails.getUrl(), parameters);
      }
      return null;
    } catch (WingsException e) {
      throw e;
    } catch (Exception ex) {
      throw new WingsException(INVALID_ARTIFACT_SERVER, USER)
          .addParam("message", "Error in fetching builds from jenkins server. Reason:" + Misc.getMessage(ex));
    }
  }

  private JobParameter getJobParameter(ParametersDefinitionProperty pdProperty) {
    JobParameter jobParameter = new JobParameter();
    jobParameter.setName(pdProperty.getName());
    jobParameter.setDescription(pdProperty.getDescription());
    if (pdProperty.getDefaultParameterValue() != null) {
      jobParameter.setDefaultValue(pdProperty.getDefaultParameterValue().getValue());
    }
    if (pdProperty.getChoices() != null) {
      jobParameter.setOptions(pdProperty.getChoices());
    }
    if (BooleanParameterDefinition.name().equals(pdProperty.getType())) {
      List<String> booleanValues = new ArrayList<>();
      booleanValues.add("true");
      booleanValues.add("false");
      jobParameter.setOptions(booleanValues);
    }
    return jobParameter;
  }

  @Override
  public boolean validateArtifactSource(JenkinsConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes) {
    return true;
  }

  @Override
  public Map<String, String> getBuckets(
      JenkinsConfig jenkinsConfig, String projectId, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by Jenkins Artifact Stream");
  }
}

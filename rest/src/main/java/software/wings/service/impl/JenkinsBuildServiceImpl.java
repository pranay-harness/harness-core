package software.wings.service.impl;

import static software.wings.utils.HttpUtil.connectableHttpUrl;
import static software.wings.utils.HttpUtil.validUrl;
import static software.wings.utils.Validator.equalCheck;

import com.google.common.collect.Lists;
import com.google.inject.Singleton;

import com.offbytwo.jenkins.model.Artifact;
import com.offbytwo.jenkins.model.JobWithDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.jenkins.JenkinsFactory;
import software.wings.service.intfc.JenkinsBuildService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;

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
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private JenkinsFactory jenkinsFactory;

  @Override
  public List<BuildDetails> getBuilds(
      String appId, ArtifactStreamAttributes artifactStreamAttributes, JenkinsConfig config) {
    logger.info("In getBuilds: App Id {} and ArtifactStreamAttributes {} and Jenkins config {} and password {}", appId,
        artifactStreamAttributes, config, config.getPassword());
    return getBuildDetails(artifactStreamAttributes, appId, config);
  }

  private List<BuildDetails> getBuildDetails(
      ArtifactStreamAttributes artifactStreamAttributes, String appId, JenkinsConfig jenkinsConfig) {
    logger.info("In getBuildDetails: App Id {} and ArtifactStreamAttributes {} and Jenkins config {} and password {}",
        appId, artifactStreamAttributes, jenkinsConfig, jenkinsConfig.getPassword());
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ArtifactStreamType.JENKINS.name());

    Jenkins jenkins =
        jenkinsFactory.create(jenkinsConfig.getJenkinsUrl(), jenkinsConfig.getUsername(), jenkinsConfig.getPassword());
    try {
      return jenkins.getBuildsForJob(artifactStreamAttributes.getJobName(), 50);
    } catch (IOException ex) {
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, "message", "Error in fetching builds from jenkins server", ex);
    }
  }

  @Override
  public List<String> getJobs(JenkinsConfig jenkinsConfig) {
    logger.info("In getJobs: Jenkins config {} and password {}", jenkinsConfig, jenkinsConfig.getPassword());
    Jenkins jenkins =
        jenkinsFactory.create(jenkinsConfig.getJenkinsUrl(), jenkinsConfig.getUsername(), jenkinsConfig.getPassword());
    try {
      return Lists.newArrayList(jenkins.getJobs().keySet());
    } catch (IOException e) {
      final WingsException wingsException = new WingsException(ErrorCode.JENKINS_ERROR, e);
      wingsException.addParam("message", "Error in fetching jobs from jenkins server");
      wingsException.addParam("jenkinsResponse", e.getMessage());
      throw wingsException;
    }
  }

  @Override
  public List<String> getArtifactPaths(String jobName, String groupId, JenkinsConfig jenkinsConfig) {
    logger.info("In getArtifactPaths: Jobname {} Jenkins config {} and password {}", jobName, jenkinsConfig,
        jenkinsConfig.getPassword());
    Jenkins jenkins =
        jenkinsFactory.create(jenkinsConfig.getJenkinsUrl(), jenkinsConfig.getUsername(), jenkinsConfig.getPassword());
    List<String> artifactPaths = new ArrayList<>();
    try {
      JobWithDetails job = jenkins.getJob(jobName);
      return Lists.newArrayList(job.getLastSuccessfulBuild()
                                    .details()
                                    .getArtifacts()
                                    .parallelStream()
                                    .map(Artifact::getRelativePath)
                                    .distinct()
                                    .collect(Collectors.toList()));
    } catch (Exception ex) {
      logger.error("Exception in generating artifact path suggestions for {}", ex);
    }
    return artifactPaths;
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(
      String appId, ArtifactStreamAttributes artifactStreamAttributes, JenkinsConfig jenkinsConfig) {
    logger.info("In getBuildDetails: App Id {} and ArtifactStreamAttributes {} and Jenkins config {} and password {}",
        appId, artifactStreamAttributes, jenkinsConfig, jenkinsConfig.getPassword());
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ArtifactStreamType.JENKINS.name());

    Jenkins jenkins =
        jenkinsFactory.create(jenkinsConfig.getJenkinsUrl(), jenkinsConfig.getUsername(), jenkinsConfig.getPassword());
    try {
      return jenkins.getLastSuccessfulBuildForJob(artifactStreamAttributes.getJobName());
    } catch (IOException ex) {
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, "message", "Error in fetching build from jenkins server", ex);
    }
  }

  @Override
  public Map<String, String> getPlans(JenkinsConfig jenkinsConfig) {
    logger.info("In getPlans: Jenkins config {} and password {}", jenkinsConfig, jenkinsConfig.getPassword());
    List<String> jobs = getJobs(jenkinsConfig);
    Map<String, String> jobKeyMap = new HashMap<>();
    if (jobs != null) {
      jobs.forEach(jobKey -> jobKeyMap.put(jobKey, jobKey));
    }
    return jobKeyMap;
  }

  @Override
  public List<String> getGroupIds(String jobName, JenkinsConfig jenkinsConfig) {
    throw new WingsException(
        ErrorCode.INVALID_REQUEST, "message", "Operation not supported by Jenkins Artifact Stream");
  }

  @Override
  public boolean validateArtifactServer(JenkinsConfig jenkinsConfig) {
    if (!validUrl(jenkinsConfig.getJenkinsUrl())) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message", "Jenkins URL must be a valid URL");
    }

    if (!connectableHttpUrl(jenkinsConfig.getJenkinsUrl())) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message",
          "Could not reach Jenkins Server at : " + jenkinsConfig.getJenkinsUrl());
    }

    Jenkins jenkins =
        jenkinsFactory.create(jenkinsConfig.getJenkinsUrl(), jenkinsConfig.getUsername(), jenkinsConfig.getPassword());
    boolean running = jenkins.isRunning();
    if (!running) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message", "Invalid Jenkins credentials");
    }
    return running;
  }

  @Override
  public boolean validateArtifactSource(JenkinsConfig config, ArtifactStreamAttributes artifactStreamAttributes) {
    return true;
  }
}

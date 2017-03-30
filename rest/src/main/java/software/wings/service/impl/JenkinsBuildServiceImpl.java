package software.wings.service.impl;

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
   * The constant ARTIFACT_STREAM_NAME.
   */
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private JenkinsFactory jenkinsFactory;

  @Override
  public List<BuildDetails> getBuilds(
      String appId, ArtifactStreamAttributes artifactStreamAttributes, JenkinsConfig config) {
    return getBuildDetails(artifactStreamAttributes, appId, config);
  }

  private List<BuildDetails> getBuildDetails(
      ArtifactStreamAttributes artifactStreamAttributes, String appId, JenkinsConfig jenkinsConfig) {
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
    Jenkins jenkins =
        jenkinsFactory.create(jenkinsConfig.getJenkinsUrl(), jenkinsConfig.getUsername(), jenkinsConfig.getPassword());
    try {
      return Lists.newArrayList(jenkins.getJobs().keySet());
    } catch (IOException e) {
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, "message", "Error in fetching jobs from jenkins server", e);
    }
  }

  @Override
  public List<String> getArtifactPaths(String jobName, JenkinsConfig jenkinsConfig) {
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
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ArtifactStreamType.JENKINS);

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
    List<String> jobs = getJobs(jenkinsConfig);
    Map<String, String> jobKeyMap = new HashMap<>();
    if (jobs != null) {
      jobs.forEach(jobKey -> jobKeyMap.put(jobKey, jobKey));
    }
    return jobKeyMap;
  }
}

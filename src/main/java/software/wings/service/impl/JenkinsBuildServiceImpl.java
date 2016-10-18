package software.wings.service.impl;

import static software.wings.utils.Validator.equalCheck;
import static software.wings.utils.Validator.notNullCheck;

import com.offbytwo.jenkins.model.Artifact;
import com.offbytwo.jenkins.model.JobWithDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.ErrorCodes;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStream.SourceType;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.beans.artifact.JenkinsConfig;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.jenkins.JenkinsFactory;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.JenkinsBuildService;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 5/13/16.
 */
public class JenkinsBuildServiceImpl implements JenkinsBuildService {
  /**
   * The constant ARTIFACT_STREAM_ID.
   */
  public static final String ARTIFACT_STREAM_ID = "artifactStreamId";
  /**
   * The constant APP_ID.
   */
  public static final String APP_ID = "appId";
  /**
   * The constant ARTIFACT_STREAM_NAME.
   */
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private JenkinsFactory jenkinsFactory;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private MainConfiguration configuration;

  @Override
  public List<BuildDetails> getBuilds(String appId, String artifactStreamId, JenkinsConfig jenkinsConfig) {
    return getBuildDetails(artifactStreamId, appId, jenkinsConfig);
  }

  private List<BuildDetails> getBuildDetails(String appId, String artifactStreamId, JenkinsConfig jenkinsConfig) {
    ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId, appId);
    notNullCheck("artifactStream", artifactStream);
    equalCheck(artifactStream.getSourceType(), SourceType.JENKINS);

    JenkinsArtifactStream jenkinsArtifactSource = ((JenkinsArtifactStream) artifactStream);

    Jenkins jenkins =
        jenkinsFactory.create(jenkinsConfig.getJenkinsUrl(), jenkinsConfig.getUsername(), jenkinsConfig.getPassword());
    try {
      return jenkins.getBuildsForJob(jenkinsArtifactSource.getJobname(), configuration.getJenkinsBuildQuerySize());
    } catch (IOException ex) {
      throw new WingsException(ErrorCodes.UNKNOWN_ERROR, "message", "Error in fetching builds from jenkins server");
    }
  }

  @Override
  public Set<String> getJobs(JenkinsConfig jenkinsConfig) {
    Jenkins jenkins =
        jenkinsFactory.create(jenkinsConfig.getJenkinsUrl(), jenkinsConfig.getUsername(), jenkinsConfig.getPassword());
    try {
      return jenkins.getJobs().keySet();
    } catch (IOException e) {
      throw new WingsException(ErrorCodes.UNKNOWN_ERROR, "message", "Error in fetching jobs from jenkins server");
    }
  }

  @Override
  public Set<String> getArtifactPaths(String jobName, JenkinsConfig jenkinsConfig) {
    Jenkins jenkins =
        jenkinsFactory.create(jenkinsConfig.getJenkinsUrl(), jenkinsConfig.getUsername(), jenkinsConfig.getPassword());
    Set<String> artifactPaths = new HashSet<>();
    try {
      JobWithDetails job = jenkins.getJob(jobName);
      return job.getLastSuccessfulBuild()
          .details()
          .getArtifacts()
          .parallelStream()
          .map(Artifact::getRelativePath)
          .collect(Collectors.toSet());
    } catch (Exception ex) {
      logger.error("Exception in generating artifact path suggestions for {}", ex);
    }
    return artifactPaths;
  }
}

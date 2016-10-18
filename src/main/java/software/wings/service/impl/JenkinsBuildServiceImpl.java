package software.wings.service.impl;

import static software.wings.utils.Validator.equalCheck;
import static software.wings.utils.Validator.notNullCheck;

import com.offbytwo.jenkins.model.Artifact;
import com.offbytwo.jenkins.model.JobWithDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.artifact.ArtifactSource;
import software.wings.beans.artifact.ArtifactSource.SourceType;
import software.wings.beans.ErrorCodes;
import software.wings.beans.artifact.JenkinsArtifactSource;
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
import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by peeyushaggarwal on 5/13/16.
 */
public class JenkinsBuildServiceImpl implements JenkinsBuildService {
  /**
   * The constant RELEASE_ID.
   */
  public static final String ARTIFACT_SOURCE_ID = "artifactSourceId";
  /**
   * The constant APP_ID.
   */
  public static final String APP_ID = "appId";
  /**
   * The constant ARTIFACT_SOURCE_NAME.
   */
  public static final String ARTIFACT_SOURCE_NAME = "artifactSourceName";
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private JenkinsFactory jenkinsFactory;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private MainConfiguration configuration;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.JenkinsBuildService#getBuilds(javax.ws.rs.core.MultivaluedMap,
   * software.wings.beans.artifact.JenkinsConfig)
   */
  @Override
  public List<BuildDetails> getBuilds(MultivaluedMap<String, String> queryParameters, JenkinsConfig jenkinsConfig)
      throws IOException {
    String artifactSourceId = queryParameters.getFirst(ARTIFACT_SOURCE_ID);
    String appId = queryParameters.getFirst(APP_ID);
    String artifactSourceName = queryParameters.getFirst(ARTIFACT_SOURCE_NAME);

    return getBuildDetails(jenkinsConfig, appId, artifactSourceId, artifactSourceName);
  }

  @Override
  public List<BuildDetails> getBuilds(
      String appId, String releaseId, String artifactSourceName, JenkinsConfig jenkinsConfig) {
    return getBuildDetails(jenkinsConfig, appId, releaseId, artifactSourceName);
  }

  private List<BuildDetails> getBuildDetails(
      JenkinsConfig jenkinsConfig, String appId, String artifactSourceId, String artifactSourceName) {
    notNullCheck(ARTIFACT_SOURCE_ID, artifactSourceId);
    notNullCheck(APP_ID, appId);
    notNullCheck(ARTIFACT_SOURCE_NAME, artifactSourceName);

    ArtifactSource artifactSource = artifactStreamService.get(artifactSourceId, appId);
    notNullCheck("artifactSource", artifactSource);
    equalCheck(artifactSource.getSourceType(), SourceType.JENKINS);

    JenkinsArtifactSource jenkinsArtifactSource = ((JenkinsArtifactSource) artifactSource);

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

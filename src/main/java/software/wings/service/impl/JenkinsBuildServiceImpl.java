package software.wings.service.impl;

import static software.wings.utils.Validator.equalCheck;
import static software.wings.utils.Validator.notNullCheck;

import software.wings.app.MainConfiguration;
import software.wings.beans.ArtifactSource;
import software.wings.beans.ArtifactSource.SourceType;
import software.wings.beans.JenkinsArtifactSource;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.Release;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.jenkins.JenkinsFactory;
import software.wings.service.intfc.JenkinsBuildService;
import software.wings.service.intfc.ReleaseService;

import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 5/13/16.
 */
public class JenkinsBuildServiceImpl implements JenkinsBuildService {
  /**
   * The constant RELEASE_ID.
   */
  public static final String RELEASE_ID = "releaseId";
  /**
   * The constant APP_ID.
   */
  public static final String APP_ID = "appId";
  /**
   * The constant ARTIFACT_SOURCE_NAME.
   */
  public static final String ARTIFACT_SOURCE_NAME = "artifactSourceName";
  @Inject private JenkinsFactory jenkinsFactory;

  @Inject private ReleaseService releaseService;

  @Inject private MainConfiguration configuration;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.JenkinsBuildService#getBuilds(javax.ws.rs.core.MultivaluedMap,
   * software.wings.beans.JenkinsConfig)
   */
  @Override
  public List<BuildDetails> getBuilds(MultivaluedMap<String, String> queryParameters, JenkinsConfig jenkinsConfig)
      throws IOException {
    String releaseId = queryParameters.getFirst(RELEASE_ID);
    String appId = queryParameters.getFirst(APP_ID);
    String artifactSourceName = queryParameters.getFirst(ARTIFACT_SOURCE_NAME);

    notNullCheck(RELEASE_ID, releaseId);
    notNullCheck(APP_ID, appId);
    notNullCheck(ARTIFACT_SOURCE_NAME, artifactSourceName);

    Release release = releaseService.get(releaseId, appId);
    notNullCheck("release", release);

    ArtifactSource artifactSource = release.get(artifactSourceName);

    notNullCheck("artifactSource", artifactSource);
    equalCheck(artifactSource.getSourceType(), SourceType.JENKINS);

    JenkinsArtifactSource jenkinsArtifactSource = ((JenkinsArtifactSource) artifactSource);

    Jenkins jenkins =
        jenkinsFactory.create(jenkinsConfig.getJenkinsUrl(), jenkinsConfig.getUsername(), jenkinsConfig.getPassword());
    return jenkins.getBuildsForJob(jenkinsArtifactSource.getJobname(), configuration.getJenkinsBuildQuerySize());
  }
}

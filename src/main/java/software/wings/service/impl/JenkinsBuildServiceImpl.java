package software.wings.service.impl;

import static software.wings.utils.Validator.equalCheck;
import static software.wings.utils.Validator.notNullCheck;

import software.wings.app.MainConfiguration;
import software.wings.beans.ArtifactSource;
import software.wings.beans.ArtifactSource.SourceType;
import software.wings.beans.JenkinsArtifactSource;
import software.wings.beans.Release;
import software.wings.helpers.ext.BuildDetails;
import software.wings.helpers.ext.Jenkins;
import software.wings.helpers.ext.JenkinsFactory;
import software.wings.service.intfc.JenkinsBuildService;
import software.wings.service.intfc.ReleaseService;

import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by peeyushaggarwal on 5/13/16.
 */
public class JenkinsBuildServiceImpl implements JenkinsBuildService {
  public static final String RELEASE_ID = "releaseId";
  public static final String APP_ID = "appId";
  public static final String ARTIFACT_SOURCE_NAME = "artifactSourceName";
  @Inject private JenkinsFactory jenkinsFactory;

  @Inject private ReleaseService releaseService;

  @Inject private MainConfiguration configuration;

  @Override
  public List<BuildDetails> getBuilds(MultivaluedMap<String, String> queryParameters) throws IOException {
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

    Jenkins jenkins = jenkinsFactory.create(jenkinsArtifactSource.getJenkinsUrl(), jenkinsArtifactSource.getUsername(),
        jenkinsArtifactSource.getPassword());
    return jenkins.getBuildsForJob(jenkinsArtifactSource.getJobname(), configuration.getJenkinsBuildQuerySize());
  }
}

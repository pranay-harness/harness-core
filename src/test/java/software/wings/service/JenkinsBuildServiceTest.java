package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.beans.JenkinsArtifactSource.Builder.aJenkinsArtifactSource;
import static software.wings.beans.Release.ReleaseBuilder.aRelease;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;
import static software.wings.service.impl.JenkinsBuildServiceImpl.APP_ID;
import static software.wings.service.impl.JenkinsBuildServiceImpl.ARTIFACT_SOURCE_NAME;
import static software.wings.service.impl.JenkinsBuildServiceImpl.RELEASE_ID;

import com.google.common.collect.Lists;

import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.ArtifactSource.ArtifactType;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.jenkins.JenkinsFactory;
import software.wings.service.intfc.JenkinsBuildService;

import java.io.IOException;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 5/13/16.
 */
public class JenkinsBuildServiceTest extends WingsBaseTest {
  private static final String releaseId = "RELEASE_ID";
  private static final String appId = "APP_ID";
  private static final String artifactSourceName = "job1";

  @Mock private JenkinsFactory jenkinsFactory;

  @Mock private Jenkins jenkins;

  @InjectMocks @Inject private JenkinsBuildService jenkinsBuildService;

  @Inject private WingsPersistence wingsPersistence;

  /**
   * setups all mocks for test.
   */
  @Before
  public void setupMocks() throws IOException {
    when(jenkinsFactory.create(anyString(), anyString(), anyString())).thenReturn(jenkins);
    when(jenkins.getBuildsForJob(anyString(), anyInt()))
        .thenReturn(Lists.newArrayList(aBuildDetails().withNumber(67).withRevision("1bfdd117").build(),
            aBuildDetails().withNumber(65).withRevision("1bfdd117").build(),
            aBuildDetails().withNumber(64).withRevision("1bfdd117").build(),
            aBuildDetails().withNumber(63).withRevision("1bfdd117").build()));
  }

  /**
   * creates data in db for test.
   */
  @Before
  public void setupData() {
    wingsPersistence.save(
        aRelease()
            .withAppId(appId)
            .withUuid(releaseId)
            .withReleaseName("Release 1.1")
            .withArtifactSources(Lists.newArrayList(aJenkinsArtifactSource()
                                                        .withJenkinsUrl("http://jenkins")
                                                        .withArtifactType(ArtifactType.WAR)
                                                        .withUsername("username")
                                                        .withPassword("password")
                                                        .withJobname(artifactSourceName)
                                                        .withArtifactPathServices(Lists.newArrayList())
                                                        .build()))
            .build());
  }

  @Test
  public void shouldFailValidationWhenReleaseIdIsNull() throws IOException {
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> jenkinsBuildService.getBuilds(new MultivaluedStringMap()));
  }

  @Test
  public void shouldFailValidationWhenArtifactSourceNameIsNull() throws IOException {
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> jenkinsBuildService.getBuilds(new MultivaluedStringMap() {
          {
            putSingle(RELEASE_ID, releaseId);
            putSingle(APP_ID, appId);
          }
        }));
  }

  @Test
  public void shouldFailValidationWhenReleaseDoesNotExists() throws IOException {
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> jenkinsBuildService.getBuilds(new MultivaluedStringMap() {
          {
            putSingle(RELEASE_ID, "BAD_RELEASE_ID");
            putSingle(APP_ID, appId);
            putSingle(ARTIFACT_SOURCE_NAME, artifactSourceName);
          }
        }));
  }

  @Test
  public void shouldFailValidationWhenJobDoesNotExists() throws IOException {
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> jenkinsBuildService.getBuilds(new MultivaluedStringMap() {
          {
            putSingle(RELEASE_ID, releaseId);
            putSingle(APP_ID, appId);
            putSingle(ARTIFACT_SOURCE_NAME, "job2");
          }
        }));
  }

  @Test
  public void shouldReturnListOfBuilds() throws IOException {
    assertThat(jenkinsBuildService.getBuilds(new MultivaluedStringMap() {
      {
        putSingle(RELEASE_ID, releaseId);
        putSingle(APP_ID, appId);
        putSingle(ARTIFACT_SOURCE_NAME, artifactSourceName);
      }
    }))
        .hasSize(4)
        .extracting(BuildDetails::getNumber, BuildDetails::getRevision)
        .containsExactly(tuple(67, "1bfdd117"), tuple(65, "1bfdd117"), tuple(64, "1bfdd117"), tuple(63, "1bfdd117"));
  }
}

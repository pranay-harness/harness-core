package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static software.wings.beans.JenkinsArtifactSource.Builder.aJenkinsArtifactSource;
import static software.wings.beans.JenkinsConfig.Builder.aJenkinsConfig;
import static software.wings.beans.Release.Builder.aRelease;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;
import static software.wings.service.impl.JenkinsBuildServiceImpl.APP_ID;
import static software.wings.service.impl.JenkinsBuildServiceImpl.ARTIFACT_SOURCE_NAME;
import static software.wings.service.impl.JenkinsBuildServiceImpl.RELEASE_ID;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import com.offbytwo.jenkins.model.Artifact;
import com.offbytwo.jenkins.model.Job;
import com.offbytwo.jenkins.model.JobWithDetails;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.JenkinsConfig;
import software.wings.common.UUIDGenerator;
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
  private static final String releaseId = UUIDGenerator.getUuid();
  private static final String appId = UUIDGenerator.getUuid();
  private static final String artifactSourceName = "job1";
  private static final JenkinsConfig jenkinsConfig =
      aJenkinsConfig().withJenkinsUrl("http://jenkins").withUsername("username").withPassword("password").build();

  @Mock private JenkinsFactory jenkinsFactory;

  @Mock private Jenkins jenkins;

  @InjectMocks @Inject private JenkinsBuildService jenkinsBuildService;

  @Inject private WingsPersistence wingsPersistence;

  /**
   * setups all mocks for test.
   *
   * @throws IOException Signals that an I/O exception has occurred.
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
                                                        .withJenkinsSettingId("")
                                                        .withSourceName(artifactSourceName)
                                                        .withJobname(artifactSourceName)
                                                        .withArtifactPathServices(Lists.newArrayList())
                                                        .build()))
            .build());
  }

  /**
   * Should fail validation when release id is null.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  public void shouldFailValidationWhenReleaseIdIsNull() throws IOException {
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> jenkinsBuildService.getBuilds(new MultivaluedStringMap(), new JenkinsConfig()));
  }

  /**
   * Should fail validation when artifact source name is null.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  public void shouldFailValidationWhenArtifactSourceNameIsNull() throws IOException {
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> jenkinsBuildService.getBuilds(new MultivaluedStringMap() {
          {
            putSingle(RELEASE_ID, releaseId);
            putSingle(APP_ID, appId);
          }
        }, jenkinsConfig));
  }

  /**
   * Should fail validation when release does not exists.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  public void shouldFailValidationWhenReleaseDoesNotExists() throws IOException {
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> jenkinsBuildService.getBuilds(new MultivaluedStringMap() {
          {
            putSingle(RELEASE_ID, "BAD_RELEASE_ID");
            putSingle(APP_ID, appId);
            putSingle(ARTIFACT_SOURCE_NAME, artifactSourceName);
          }
        }, jenkinsConfig));
  }

  /**
   * Should fail validation when job does not exists.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  public void shouldFailValidationWhenJobDoesNotExists() throws IOException {
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> jenkinsBuildService.getBuilds(new MultivaluedStringMap() {
          {
            putSingle(RELEASE_ID, releaseId);
            putSingle(APP_ID, appId);
            putSingle(ARTIFACT_SOURCE_NAME, "job2");
          }
        }, jenkinsConfig));
  }

  /**
   * Should return list of builds.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  public void shouldReturnListOfBuilds() throws IOException {
    assertThat(jenkinsBuildService.getBuilds(
                   new MultivaluedStringMap() {
                     {
                       putSingle(RELEASE_ID, releaseId);
                       putSingle(APP_ID, appId);
                       putSingle(ARTIFACT_SOURCE_NAME, artifactSourceName);
                     }
                   },
                   jenkinsConfig))
        .hasSize(4)
        .extracting(BuildDetails::getNumber, BuildDetails::getRevision)
        .containsExactly(tuple(67, "1bfdd117"), tuple(65, "1bfdd117"), tuple(64, "1bfdd117"), tuple(63, "1bfdd117"));
  }

  @Test
  public void shouldFetchJobNames() throws IOException {
    when(jenkins.getJobs()).thenReturn(ImmutableMap.of("jobName", new Job()));
    assertThat(jenkinsBuildService.getJobs(jenkinsConfig)).isEqualTo(ImmutableSet.of("jobName"));
  }

  @Test
  public void shouldFetchArtifactPaths() throws IOException {
    JobWithDetails jobWithDetails = Mockito.mock(JobWithDetails.class, RETURNS_DEEP_STUBS);
    Artifact artifact = new Artifact();
    artifact.setRelativePath("relativePath");
    when(jenkins.getJob("jobName")).thenReturn(jobWithDetails);
    when(jobWithDetails.getLastSuccessfulBuild().details().getArtifacts()).thenReturn(ImmutableList.of(artifact));
    assertThat(jenkinsBuildService.getArtifactPaths("jobName", jenkinsConfig))
        .isEqualTo(ImmutableSet.of("relativePath"));
  }
}

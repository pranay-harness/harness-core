package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static software.wings.beans.JenkinsConfig.Builder.aJenkinsConfig;
import static software.wings.beans.artifact.JenkinsArtifactStream.Builder.aJenkinsArtifactStream;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_NAME;
import static software.wings.utils.WingsTestConstants.BUILD_JOB_NAME;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import com.offbytwo.jenkins.model.Artifact;
import com.offbytwo.jenkins.model.Job;
import com.offbytwo.jenkins.model.JobWithDetails;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.artifact.JenkinsArtifactStream;
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
  private static final JenkinsConfig jenkinsConfig =
      aJenkinsConfig().withJenkinsUrl("http://jenkins").withUsername("username").withPassword("password").build();

  @Mock private JenkinsFactory jenkinsFactory;

  @Mock private Jenkins jenkins;

  @InjectMocks @Inject private JenkinsBuildService jenkinsBuildService;

  private static final JenkinsArtifactStream jenkinsArtifactStream = aJenkinsArtifactStream()
                                                                         .withUuid(ARTIFACT_STREAM_ID)
                                                                         .withAppId(APP_ID)
                                                                         .withSettingId("")
                                                                         .withSourceName(ARTIFACT_STREAM_NAME)
                                                                         .withJobname("job1")
                                                                         .build();

  /**
   * setups all mocks for test.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Before
  public void setupMocks() throws IOException {
    when(jenkinsFactory.create(anyString(), anyString(), anyString())).thenReturn(jenkins);
    when(jenkins.getBuildsForJob(eq("job1"), anyInt()))
        .thenReturn(Lists.newArrayList(aBuildDetails().withNumber("67").withRevision("1bfdd117").build(),
            aBuildDetails().withNumber("65").withRevision("1bfdd117").build(),
            aBuildDetails().withNumber("64").withRevision("1bfdd117").build(),
            aBuildDetails().withNumber("63").withRevision("1bfdd117").build()));
  }

  /**
   * Should fail validation when job does not exists.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @Ignore // TODO:: remove ignore
  public void shouldFailValidationWhenJobDoesNotExists() throws IOException {
    jenkinsArtifactStream.setJobname("job2");
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(()
                        -> jenkinsBuildService.getBuilds(
                            APP_ID, jenkinsArtifactStream.getArtifactStreamAttributes(), jenkinsConfig));
  }

  /**
   * Should return list of builds.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  public void shouldReturnListOfBuilds() throws IOException {
    assertThat(
        jenkinsBuildService.getBuilds(APP_ID, jenkinsArtifactStream.getArtifactStreamAttributes(), jenkinsConfig))
        .hasSize(4)
        .extracting(BuildDetails::getNumber, BuildDetails::getRevision)
        .containsExactly(
            tuple("67", "1bfdd117"), tuple("65", "1bfdd117"), tuple("64", "1bfdd117"), tuple("63", "1bfdd117"));
  }

  /**
   * Should fetch job names.
   *
   * @throws IOException the io exception
   */
  @Test
  public void shouldFetchJobNames() throws IOException {
    when(jenkins.getJobs()).thenReturn(ImmutableMap.of("jobName", new Job()));
    assertThat(jenkinsBuildService.getJobs(jenkinsConfig)).containsExactly("jobName");
  }

  /**
   * Should fetch artifact paths.
   *
   * @throws IOException the io exception
   */
  @Test
  public void shouldFetchArtifactPaths() throws IOException {
    JobWithDetails jobWithDetails = Mockito.mock(JobWithDetails.class, RETURNS_DEEP_STUBS);
    Artifact artifact = new Artifact();
    artifact.setRelativePath("relativePath");
    when(jenkins.getJob(BUILD_JOB_NAME)).thenReturn(jobWithDetails);
    when(jobWithDetails.getLastSuccessfulBuild().details().getArtifacts()).thenReturn(ImmutableList.of(artifact));
    assertThat(jenkinsBuildService.getArtifactPaths(BUILD_JOB_NAME, jenkinsConfig)).containsExactly("relativePath");
  }
}

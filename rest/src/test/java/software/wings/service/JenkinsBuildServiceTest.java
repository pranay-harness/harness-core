package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;
import static software.wings.helpers.ext.jenkins.model.ParametersDefinitionProperty.builder;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_NAME;
import static software.wings.utils.WingsTestConstants.BUILD_JOB_NAME;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.offbytwo.jenkins.model.Artifact;
import com.offbytwo.jenkins.model.JobWithDetails;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.ErrorCode;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.jenkins.JenkinsFactory;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.helpers.ext.jenkins.model.JobProperty;
import software.wings.helpers.ext.jenkins.model.JobWithExtendedDetails;
import software.wings.helpers.ext.jenkins.model.ParametersDefinitionProperty.DefaultParameterValue;
import software.wings.service.impl.jenkins.JenkinsUtil;
import software.wings.service.intfc.JenkinsBuildService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Created by peeyushaggarwal on 5/13/16.
 */
public class JenkinsBuildServiceTest extends WingsBaseTest {
  private static final JenkinsConfig jenkinsConfig = JenkinsConfig.builder()
                                                         .jenkinsUrl("http://jenkins")
                                                         .username("username")
                                                         .password("password".toCharArray())
                                                         .accountId(ACCOUNT_ID)
                                                         .build();

  @Mock private JenkinsFactory jenkinsFactory;

  @Mock private Jenkins jenkins;

  @Inject @InjectMocks JenkinsUtil jenkinsUtil;

  @InjectMocks @Inject private JenkinsBuildService jenkinsBuildService;

  private static final JenkinsArtifactStream jenkinsArtifactStream = JenkinsArtifactStream.builder()
                                                                         .uuid(ARTIFACT_STREAM_ID)
                                                                         .appId(APP_ID)
                                                                         .settingId("")
                                                                         .sourceName(ARTIFACT_STREAM_NAME)
                                                                         .jobname("job1")
                                                                         .build();

  /**
   * setups all mocks for test.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Before
  public void setupMocks() throws IOException {
    when(jenkinsFactory.create(anyString(), anyString(), any(char[].class))).thenReturn(jenkins);
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
                            APP_ID, jenkinsArtifactStream.getArtifactStreamAttributes(), jenkinsConfig, null));
  }

  /**
   * Should return list of builds.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  public void shouldReturnListOfBuilds() throws IOException {
    assertThat(
        jenkinsBuildService.getBuilds(APP_ID, jenkinsArtifactStream.getArtifactStreamAttributes(), jenkinsConfig, null))
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
    when(jenkins.getJobs(anyString())).thenReturn(ImmutableList.of(new JobDetails("jobName", false)));
    List<JobDetails> jobs = jenkinsBuildService.getJobs(jenkinsConfig, null, Optional.empty());
    List<String> jobNames = jenkinsBuildService.extractJobNameFromJobDetails(jobs);
    assertThat(jobNames).containsExactly("jobName");
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
    assertThat(jenkinsBuildService.getArtifactPaths(BUILD_JOB_NAME, null, jenkinsConfig, null))
        .containsExactly("relativePath");
  }

  @Test
  public void shouldValidateInvalidUrl() {
    JenkinsConfig badJenkinsConfig = JenkinsConfig.builder()
                                         .jenkinsUrl("BAD_URL")
                                         .username("username")
                                         .password("password".toCharArray())
                                         .accountId(ACCOUNT_ID)
                                         .build();
    try {
      jenkinsBuildService.validateArtifactServer(badJenkinsConfig);
      fail("jenkinsBuildService.validateArtifactServer did not throw!!!");
    } catch (WingsException e) {
      assertThat(e.getMessage()).isEqualTo(ErrorCode.INVALID_ARTIFACT_SERVER.toString());
      assertThat(e.getParams()).isNotEmpty();
      assertThat(e.getParams().get("message")).isEqualTo("Could not reach Jenkins Server at : BAD_URL");
    }
  }

  @Test
  public void shouldTestGetJobParameters() {
    JobWithExtendedDetails jobWithDetails = Mockito.mock(JobWithExtendedDetails.class, RETURNS_DEEP_STUBS);
    when(jenkins.getJob(BUILD_JOB_NAME)).thenReturn(jobWithDetails);
    JobProperty jobProperty =
        JobProperty.builder()
            .parameterDefinitions(asList(
                builder()
                    .name("branch")
                    .defaultParameterValue(DefaultParameterValue.builder().name("branch").value("release").build())
                    .build(),
                builder()
                    .name("revision")
                    .defaultParameterValue(DefaultParameterValue.builder().name("revision").build())
                    .build(),
                builder()
                    .name("Choices")
                    .defaultParameterValue(DefaultParameterValue.builder().name("B").value("B").build())
                    .choices(asList("A", "B", "C"))
                    .build(),
                builder()
                    .name("BooleanParams")
                    .type("BooleanParameterDefinition")
                    .defaultParameterValue(DefaultParameterValue.builder().name("B").value("B").build())
                    .choices(asList("A", "B", "C"))
                    .build()))
            .build();
    when(jobWithDetails.getProperties()).thenReturn(asList(jobProperty));
    JobDetails jobDetails = jenkinsBuildService.getJob(BUILD_JOB_NAME, jenkinsConfig, null);
    assertThat(jobDetails).isNotNull();
    assertThat(jobDetails.getParameters())
        .isNotNull()
        .extracting(JobDetails.JobParameter::getName)
        .contains("branch", "revision", "Choices", "BooleanParams");
    assertThat(jobDetails.getParameters())
        .extracting(JobDetails.JobParameter::getOptions)
        .contains(asList("A", "B", "C"), asList("true", "false"));

    assertThat(jobDetails.getParameters())
        .extracting(JobDetails.JobParameter::getDefaultValue)
        .contains("release", "B");
  }
}

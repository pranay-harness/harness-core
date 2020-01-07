package software.wings.helpers.ext.jenkins;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.harness.rule.OwnerRule.AADITI;
import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.RAMA;
import static io.harness.rule.OwnerRule.RUSHABH;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static io.harness.rule.OwnerRule.UNKNOWN;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.joor.Reflect.on;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FakeTimeLimiter;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.JobWithDetails;
import com.offbytwo.jenkins.model.QueueReference;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.artifact.ArtifactFileMetadata;
import io.harness.rule.Owner;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.helpers.ext.jenkins.model.JobProperty;
import software.wings.helpers.ext.jenkins.model.JobWithExtendedDetails;
import software.wings.helpers.ext.jenkins.model.ParametersDefinitionProperty;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The Class JenkinsTest.
 */
public class JenkinsTest extends CategoryTest {
  /**
   * The Wire mock rule.
   */
  @Rule public WireMockRule wireMockRule = new WireMockRule(8089);
  private Jenkins jenkins =
      new JenkinsImpl("http://localhost:8089", "wingsbuild", "0db28aa0f4fc0685df9a216fc7af0ca96254b7c2".toCharArray());

  public JenkinsTest() throws URISyntaxException {}

  @Before
  public void setupMocks() {
    on(jenkins).set("timeLimiter", new FakeTimeLimiter());
  }

  /**
   * Should get job from jenkins.
   *
   * @throws URISyntaxException the URI syntax exception
   * @throws IOException        Signals that an I/O exception has occurred.
   */
  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldGetJobFromJenkins() throws IOException {
    assertThat(jenkins.getJob("scheduler")).isNotNull();
  }

  /**
   * Should get child jobs from jenkins.
   *
   * @throws URISyntaxException the URI syntax exception
   * @throws IOException        Signals that an I/O exception has occurred.
   */
  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldGetJobsFromJenkins() throws IOException {
    wireMockRule.stubFor(
        get(urlEqualTo("/job/parentJob/api/json"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(
                        "{\"_class\":\"com.cloudbees.hudson.plugins.folder.Folder\",\"property\":[{},{\"_class\":\"hudson.plugins.jobConfigHistory.JobConfigHistoryProjectAction\"},{},{\"_class\":\"com.cloudbees.plugins.credentials.ViewCredentialsAction\"}],\"description\":null,\"displayName\":\"parentJob\",\"displayNameOrNull\":null,\"fullDisplayName\":\"parentJob\",\"fullName\":\"parentJob\",\"name\":\"parentJob\",\"url\":\"https://jenkins.wings.software/job/parentJob/\",\"healthReport\":[],\"jobs\":[{\"_class\":\"hudson.maven.MavenModuleSet\",\"name\":\"abcd\",\"url\":\"https://jenkins.wings.software/job/parentJob/job/abcd/\"},{\"_class\":\"hudson.maven.MavenModuleSet\",\"name\":\"parentJob_war_copy\",\"url\":\"https://jenkins.wings.software/job/parentJob/job/parentJob_war_copy/\",\"color\":\"notbuilt\"}],\"primaryView\":{\"_class\":\"hudson.model.AllView\",\"name\":\"All\",\"url\":\"https://jenkins.wings.software/job/parentJob/\"},\"views\":[{\"_class\":\"hudson.model.AllView\",\"name\":\"All\",\"url\":\"https://jenkins.wings.software/job/parentJob/\"}]}")
                    .withHeader("Content-Type", "application/json")));

    List<JobDetails> jobs = jenkins.getJobs("parentJob");
    assertThat(jobs.size() == 2).isTrue();
  }

  /**
   * Should get child jobs from jenkins.
   *
   * @throws URISyntaxException the URI syntax exception
   * @throws IOException        Signals that an I/O exception has occurred.
   */
  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldGetJobsFromJenkinsForDifferentHost() throws IOException {
    wireMockRule.stubFor(
        get(urlEqualTo("/job/parentJob/api/json"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(
                        "{\"_class\":\"com.cloudbees.hudson.plugins.folder.Folder\",\"actions\":[{},{\"_class\":\"hudson.plugins.jobConfigHistory.JobConfigHistoryProjectAction\"},{},{\"_class\":\"com.cloudbees.plugins.credentials.ViewCredentialsAction\"}],\"description\":null,\"displayName\":\"parentJob\",\"displayNameOrNull\":null,\"fullDisplayName\":\"parentJob\",\"fullName\":\"parentJob\",\"name\":\"parentJob\",\"url\":\"https://jenkins.wings.software/job/parentJob/\",\"healthReport\":[],\"jobs\":[{\"_class\":\"hudson.maven.MavenModuleSet\",\"name\":\"abcd\",\"url\":\"https://jenkins.wings.software/job/parentJob/job/abcd/\"},{\"_class\":\"hudson.maven.MavenModuleSet\",\"name\":\"parentJob_war_copy\",\"url\":\"https://jenkins.wings.software/job/parentJob/job/parentJob_war_copy/\",\"color\":\"notbuilt\"}],\"primaryView\":{\"_class\":\"hudson.model.AllView\",\"name\":\"All\",\"url\":\"https://jenkins.wings.software/job/parentJob/\"},\"views\":[{\"_class\":\"hudson.model.AllView\",\"name\":\"All\",\"url\":\"https://jenkins.wings.software/job/parentJob/\"}]}")
                    .withHeader("Content-Type", "application/json")));

    List<JobDetails> jobs = jenkins.getJobs("parentJob");
    assertThat(jobs.size() == 2).isTrue();
    assertThat(jobs.get(0).getJobName().equals("parentJob/parentJob_war_copy")).isTrue();
    assertThat(jobs.get(1).getJobName().equals("parentJob/abcd")).isTrue();
  }

  /**
   * Should return null when job does not exist.
   *
   * @throws URISyntaxException the URI syntax exception
   * @throws IOException        Signals that an I/O exception has occurred.
   */
  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldReturnNullWhenJobDoesNotExist() throws URISyntaxException, IOException {
    assertThat(jenkins.getJob("scheduler1")).isNull();
  }

  /**
   * Should return artifacts by build number.
   *
   * @throws URISyntaxException the URI syntax exception
   * @throws IOException        Signals that an I/O exception has occurred.
   */
  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldReturnArtifactsByBuildNumber() throws URISyntaxException, IOException {
    Pair<String, InputStream> fileInfo =
        jenkins.downloadArtifact("scheduler", "57", "build/libs/docker-scheduler-*.jar");
    assertThat(fileInfo.getKey()).isEqualTo("docker-scheduler-1.0-SNAPSHOT-all.jar");
    fileInfo.getValue().close();
  }

  /**
   * Should return last completed build artifacts.
   *
   * @throws URISyntaxException the URI syntax exception
   * @throws IOException        Signals that an I/O exception has occurred.
   */
  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldReturnLastCompletedBuildArtifacts() throws URISyntaxException, IOException {
    Pair<String, InputStream> fileInfo = jenkins.downloadArtifact("scheduler", "build/libs/docker-scheduler-*.jar");
    assertThat(fileInfo.getKey()).isEqualTo("docker-scheduler-1.0-SNAPSHOT-all.jar");
    fileInfo.getValue().close();
  }

  /**
   * Should return null artifact if job is missing.
   *
   * @throws URISyntaxException the URI syntax exception
   * @throws IOException        Signals that an I/O exception has occurred.
   */
  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldReturnNullArtifactIfJobIsMissing() throws URISyntaxException, IOException {
    Pair<String, InputStream> fileInfo =
        jenkins.downloadArtifact("scheduler1", "57", "build/libs/docker-scheduler-*.jar");
    assertThat(fileInfo).isNull();
  }

  /**
   * Should return null artifact if build is missing.
   *
   * @throws URISyntaxException the URI syntax exception
   * @throws IOException        Signals that an I/O exception has occurred.
   */
  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldReturnNullArtifactIfBuildIsMissing() throws URISyntaxException, IOException {
    Pair<String, InputStream> fileInfo =
        jenkins.downloadArtifact("scheduler", "-1", "build/libs/docker-scheduler-*.jar");
    assertThat(fileInfo).isNull();
  }

  /**
   * Should return null artifact when artifact path doesnot match.
   *
   * @throws URISyntaxException the URI syntax exception
   * @throws IOException        Signals that an I/O exception has occurred.
   */
  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldReturnNullArtifactWhenArtifactPathDoesnotMatch() throws URISyntaxException, IOException {
    Pair<String, InputStream> fileInfo = jenkins.downloadArtifact("scheduler", "57", "build/libs/dummy-*.jar");
    assertThat(fileInfo).isNull();
  }

  /**
   * Should get last n build details for git jobs.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldGetLastNBuildDetailsForGitJobs() throws IOException {
    List<BuildDetails> buildDetails = jenkins.getBuildsForJob(
        "scheduler", asList("build/libs/docker-scheduler-1.0-SNAPSHOT-all.jar", "todolist.war"), 5);
    assertThat(buildDetails)
        .hasSize(4)
        .extracting(BuildDetails::getNumber, BuildDetails::getRevision)
        .containsExactly(tuple("67", "1bfdd1174d41e1f32cbfc287f18c3cc040ca90e3"),
            tuple("65", "1bfdd1174d41e1f32cbfc287f18c3cc040ca90e3"),
            tuple("64", "1bfdd1174d41e1f32cbfc287f18c3cc040ca90e3"),
            tuple("63", "1bfdd1174d41e1f32cbfc287f18c3cc040ca90e3"));

    buildDetails.forEach(buildDetails1 -> {
      String url = "http://localhost:8089/job/scheduler/" + buildDetails1.getNumber()
          + "/artifact/build/libs/docker-scheduler-1.0-SNAPSHOT-all.jar";
      assertThat(buildDetails1.getArtifactFileMetadataList()).isNotEmpty();
      assertThat(buildDetails1.getArtifactFileMetadataList())
          .extracting(ArtifactFileMetadata::getFileName, ArtifactFileMetadata::getUrl)
          .containsExactly(tuple("docker-scheduler-1.0-SNAPSHOT-all.jar", url));
    });
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldGetLastSuccessfulBuildForGitJob() throws IOException {
    BuildDetails buildDetails =
        jenkins.getLastSuccessfulBuildForJob("scheduler", asList("build/libs/docker-scheduler-1.0-SNAPSHOT-all.jar"));
    assertThat(buildDetails).isNotNull();
    assertThat(buildDetails.getNumber()).isEqualTo("67");
    assertThat(buildDetails.getRevision()).isEqualTo("1bfdd1174d41e1f32cbfc287f18c3cc040ca90e3");
    assertThat(buildDetails.getArtifactFileMetadataList().size()).isEqualTo(1);
    assertThat(buildDetails.getArtifactFileMetadataList().get(0).getFileName())
        .isEqualTo("docker-scheduler-1.0-SNAPSHOT-all.jar");
    assertThat(buildDetails.getArtifactFileMetadataList().get(0).getUrl())
        .isEqualTo("http://localhost:8089/job/scheduler/67/artifact/build/libs/docker-scheduler-1.0-SNAPSHOT-all.jar");
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void shouldGetNullLastSuccessfulBuildForNonExistingGitJob() throws IOException {
    BuildDetails buildDetails =
        jenkins.getLastSuccessfulBuildForJob("scheduler1", asList("build/libs/docker-scheduler-1.0-SNAPSHOT-all.jar"));
    assertThat(buildDetails).isNull();
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldGetLastNBuildDetailsForSvnJobs() throws IOException {
    List<BuildDetails> buildDetails =
        jenkins.getBuildsForJob("scheduler-svn", asList("build/libs/docker-scheduler-1.0-SNAPSHOT-all.jar"), 5);
    assertThat(buildDetails)
        .hasSize(4)
        .extracting(BuildDetails::getNumber, BuildDetails::getRevision)
        .containsExactly(tuple("65", "39"), tuple("64", "39"), tuple("63", "39"), tuple("62", "39"));
    buildDetails.forEach(buildDetails1 -> {
      String url = "http://localhost:8089/job/scheduler-svn/" + buildDetails1.getNumber()
          + "/artifact/build/libs/docker-scheduler-1.0-SNAPSHOT-all.jar";
      assertThat(buildDetails1.getArtifactFileMetadataList()).isNotEmpty();
      assertThat(buildDetails1.getArtifactFileMetadataList())
          .extracting(ArtifactFileMetadata::getFileName, ArtifactFileMetadata::getUrl)
          .containsExactly(tuple("docker-scheduler-1.0-SNAPSHOT-all.jar", url));
    });
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldTriggerJobWithParameters() throws IOException {
    QueueReference queueItem = jenkins.trigger("todolist_war", ImmutableMap.of("Test", "Test"));
    assertThat(queueItem.getQueueItemUrlPart()).isNotNull();
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldFetchBuildFromQueueItem() throws IOException {
    Build build = jenkins.getBuild(new QueueReference("http://localhost:8089/queue/item/27287"));
    assertThat(build.getQueueId()).isEqualTo(27287);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldTriggerJobWithoutParameters() throws IOException {
    QueueReference queueItem = jenkins.trigger("todolist_war", Collections.emptyMap());
    assertThat(queueItem.getQueueItemUrlPart()).isNotNull();
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testJobNormalizedNames() throws Exception {
    JenkinsImpl jenkins = new JenkinsImpl("http://localhost:8080");
    assertThat(jenkins.getNormalizedName("TestJob")).isEqualTo("TestJob");
    assertThat(jenkins.getNormalizedName(null)).isNull();
    assertThat(jenkins.getNormalizedName("Test%20Job")).isEqualTo("Test Job");
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldTestGetJobParameters() {
    JobWithDetails jobWithDetails = jenkins.getJob("todolist_promot");
    assertThat(jobWithDetails).isNotNull();
    assertThat(jobWithDetails).isInstanceOf(JobWithExtendedDetails.class);
    JobWithExtendedDetails jobWithExtendedDetails = (JobWithExtendedDetails) jobWithDetails;
    assertThat(jobWithExtendedDetails).extracting(JobWithExtendedDetails::getProperties).isNotNull();

    List<ParametersDefinitionProperty> properties = jobWithExtendedDetails.getProperties()
                                                        .stream()
                                                        .map(JobProperty::getParameterDefinitions)
                                                        .filter(Objects::nonNull)
                                                        .flatMap(Collection::stream)
                                                        .collect(toList());
    assertThat(properties)
        .isNotNull()
        .extracting(ParametersDefinitionProperty::getName)
        .containsSequence("revision", "branch", "Choices", "boolean", "Credentials");
    assertThat(properties)
        .extracting(ParametersDefinitionProperty::getDefaultParameterValue)
        .extracting("value")
        .contains("release");
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldTestGetFileSize() {
    long size = jenkins.getFileSize("scheduler", "57", "build/libs/docker-scheduler-1.0-SNAPSHOT-all.jar");
    assertThat(size).isGreaterThan(0);
  }
}

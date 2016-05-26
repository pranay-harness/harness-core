package software.wings.helpers.ext.jenkins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.jenkins.JenkinsFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;
import javax.inject.Inject;

public class JenkinsTest extends WingsBaseTest {
  @Inject private JenkinsFactory jenkinsFactory;

  private Jenkins jenkins;

  @Rule public WireMockRule wireMockRule = new WireMockRule(8089);

  @Before
  public void setUp() throws URISyntaxException {
    jenkins = jenkinsFactory.create("http://localhost:8089", "admin", "admin");
  }

  @Test
  public void shouldGetJobFromJenkins() throws URISyntaxException, IOException {
    assertThat(jenkins.getJob("scheduler")).isNotNull();
  }

  @Test
  public void shouldReturnNullWhenJobDoesNotExist() throws URISyntaxException, IOException {
    assertThat(jenkins.getJob("scheduler1")).isNull();
  }

  @Test
  public void shouldReturnArtifactsByBuildNumber() throws URISyntaxException, IOException {
    Pair<String, InputStream> fileInfo =
        jenkins.downloadArtifact("scheduler", "57", "build/libs/docker-scheduler-*.jar");
    assertThat(fileInfo.getKey()).isEqualTo("docker-scheduler-1.0-SNAPSHOT-all.jar");
    IOUtils.closeQuietly(fileInfo.getValue());
  }

  @Test
  public void shouldReturnLastCompletedBuildArtifacts() throws URISyntaxException, IOException {
    Pair<String, InputStream> fileInfo = jenkins.downloadArtifact("scheduler", "build/libs/docker-scheduler-*.jar");
    assertThat(fileInfo.getKey()).isEqualTo("docker-scheduler-1.0-SNAPSHOT-all.jar");
    IOUtils.closeQuietly(fileInfo.getValue());
  }

  @Test
  public void shouldReturnNullArtifactIfJobIsMissing() throws URISyntaxException, IOException {
    Pair<String, InputStream> fileInfo =
        jenkins.downloadArtifact("scheduler1", "57", "build/libs/docker-scheduler-*.jar");
    assertThat(fileInfo).isNull();
  }

  @Test
  public void shouldReturnNullArtifactIfBuildIsMissing() throws URISyntaxException, IOException {
    Pair<String, InputStream> fileInfo =
        jenkins.downloadArtifact("scheduler", "-1", "build/libs/docker-scheduler-*.jar");
    assertThat(fileInfo).isNull();
  }

  @Test
  public void shouldReturnNullArtifactWhenArtifactPathDoesnotMatch() throws URISyntaxException, IOException {
    Pair<String, InputStream> fileInfo = jenkins.downloadArtifact("scheduler", "57", "build/libs/dummy-*.jar");
    assertThat(fileInfo).isNull();
  }

  @Test
  public void shouldGetLastNBuildDetailsForGitJobs() throws URISyntaxException, IOException {
    List<BuildDetails> buildDetails = jenkins.getBuildsForJob("scheduler", 5);
    assertThat(buildDetails)
        .hasSize(4)
        .extracting(BuildDetails::getNumber, BuildDetails::getRevision)
        .containsExactly(tuple(67, "1bfdd117"), tuple(65, "1bfdd117"), tuple(64, "1bfdd117"), tuple(63, "1bfdd117"));
  }

  @Test
  public void shouldGetLastNBuildDetailsForSvnJobs() throws URISyntaxException, IOException {
    List<BuildDetails> buildDetails = jenkins.getBuildsForJob("scheduler-svn", 5);
    assertThat(buildDetails)
        .hasSize(4)
        .extracting(BuildDetails::getNumber, BuildDetails::getRevision)
        .containsExactly(tuple(65, "39"), tuple(64, "39"), tuple(63, "39"), tuple(62, "39"));
  }
}

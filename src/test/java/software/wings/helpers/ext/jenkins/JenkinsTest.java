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

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;
import javax.inject.Inject;

// TODO: Auto-generated Javadoc

/**
 * The Class JenkinsTest.
 */
public class JenkinsTest extends WingsBaseTest {
  @Rule public WireMockRule wireMockRule = new WireMockRule(8089);
  @Inject private JenkinsFactory jenkinsFactory;
  private Jenkins jenkins;

  /**
   * Sets the up.
   *
   * @throws URISyntaxException the URI syntax exception
   */
  @Before
  public void setUp() throws URISyntaxException {
    jenkins = jenkinsFactory.create("http://localhost:8089", "admin", "admin");
  }

  /**
   * Should get job from jenkins.
   *
   * @throws URISyntaxException the URI syntax exception
   * @throws IOException        Signals that an I/O exception has occurred.
   */
  @Test
  public void shouldGetJobFromJenkins() throws URISyntaxException, IOException {
    assertThat(jenkins.getJob("scheduler")).isNotNull();
  }

  /**
   * Should return null when job does not exist.
   *
   * @throws URISyntaxException the URI syntax exception
   * @throws IOException        Signals that an I/O exception has occurred.
   */
  @Test
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
  public void shouldReturnArtifactsByBuildNumber() throws URISyntaxException, IOException {
    Pair<String, InputStream> fileInfo =
        jenkins.downloadArtifact("scheduler", "57", "build/libs/docker-scheduler-*.jar");
    assertThat(fileInfo.getKey()).isEqualTo("docker-scheduler-1.0-SNAPSHOT-all.jar");
    IOUtils.closeQuietly(fileInfo.getValue());
  }

  /**
   * Should return last completed build artifacts.
   *
   * @throws URISyntaxException the URI syntax exception
   * @throws IOException        Signals that an I/O exception has occurred.
   */
  @Test
  public void shouldReturnLastCompletedBuildArtifacts() throws URISyntaxException, IOException {
    Pair<String, InputStream> fileInfo = jenkins.downloadArtifact("scheduler", "build/libs/docker-scheduler-*.jar");
    assertThat(fileInfo.getKey()).isEqualTo("docker-scheduler-1.0-SNAPSHOT-all.jar");
    IOUtils.closeQuietly(fileInfo.getValue());
  }

  /**
   * Should return null artifact if job is missing.
   *
   * @throws URISyntaxException the URI syntax exception
   * @throws IOException        Signals that an I/O exception has occurred.
   */
  @Test
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
  public void shouldReturnNullArtifactWhenArtifactPathDoesnotMatch() throws URISyntaxException, IOException {
    Pair<String, InputStream> fileInfo = jenkins.downloadArtifact("scheduler", "57", "build/libs/dummy-*.jar");
    assertThat(fileInfo).isNull();
  }

  /**
   * Should get last n build details for git jobs.
   *
   * @throws URISyntaxException the URI syntax exception
   * @throws IOException        Signals that an I/O exception has occurred.
   */
  @Test
  public void shouldGetLastNBuildDetailsForGitJobs() throws URISyntaxException, IOException {
    List<BuildDetails> buildDetails = jenkins.getBuildsForJob("scheduler", 5);
    assertThat(buildDetails)
        .hasSize(4)
        .extracting(BuildDetails::getNumber, BuildDetails::getRevision)
        .containsExactly(tuple(67, "1bfdd117"), tuple(65, "1bfdd117"), tuple(64, "1bfdd117"), tuple(63, "1bfdd117"));
  }

  /**
   * Should get last n build details for svn jobs.
   *
   * @throws URISyntaxException the URI syntax exception
   * @throws IOException        Signals that an I/O exception has occurred.
   */
  @Test
  public void shouldGetLastNBuildDetailsForSvnJobs() throws URISyntaxException, IOException {
    List<BuildDetails> buildDetails = jenkins.getBuildsForJob("scheduler-svn", 5);
    assertThat(buildDetails)
        .hasSize(4)
        .extracting(BuildDetails::getNumber, BuildDetails::getRevision)
        .containsExactly(tuple(65, "39"), tuple(64, "39"), tuple(63, "39"), tuple(62, "39"));
  }
}

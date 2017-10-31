package software.wings.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_GROUP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_NAME;
import static software.wings.utils.WingsTestConstants.BUILD_JOB_NAME;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.newrelic.agent.deps.com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.ErrorCode;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.beans.config.NexusConfig;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.helpers.ext.nexus.NexusService;
import software.wings.service.intfc.NexusBuildService;
import software.wings.helpers.ext.jenkins.BuildDetails;

/**
 * Created by srinivas on 4/1/17.
 */
public class NexusBuildServiceTest extends WingsBaseTest {
  @Mock private NexusService nexusService;

  @Inject @InjectMocks private NexusBuildService nexusBuildService;

  private static final String DEFAULT_NEXUS_URL = "http://localhost:8881/nexus/";

  private NexusConfig nexusConfig =
      NexusConfig.builder().nexusUrl(DEFAULT_NEXUS_URL).username("admin").password("admin123".toCharArray()).build();

  private static final NexusArtifactStream nexusArtifactStream =
      NexusArtifactStream.Builder.aNexusArtifactStream()
          .withUuid(ARTIFACT_STREAM_ID)
          .withAppId(APP_ID)
          .withSettingId("")
          .withSourceName(ARTIFACT_STREAM_NAME)
          .withJobname(BUILD_JOB_NAME)
          .withGroupId(ARTIFACT_GROUP_ID)
          .withArtifactPaths(Stream.of(ARTIFACT_NAME).collect(Collectors.toList()))
          .build();

  @Test
  public void shouldGetPlans() {
    when(nexusService.getRepositories(nexusConfig, null))
        .thenReturn(ImmutableMap.of("snapshots", "Snapshots", "releases", "Releases"));
    Map<String, String> jobs = nexusBuildService.getPlans(nexusConfig, null);
    assertThat(jobs).hasSize(2).containsEntry("releases", "Releases");
  }

  @Test
  public void shouldGetJobs() {
    when(nexusService.getRepositories(nexusConfig, null))
        .thenReturn(ImmutableMap.of("snapshots", "Snapshots", "releases", "Releases"));
    List<JobDetails> jobs = nexusBuildService.getJobs(nexusConfig, null, Optional.empty());
    List<String> jobNames = nexusBuildService.extractJobNameFromJobDetails(jobs);
    assertThat(jobNames).hasSize(2).containsExactlyInAnyOrder("releases", "snapshots");
  }

  @Test
  public void shouldGetArtifactPaths() {
    when(nexusService.getArtifactPaths(nexusConfig, null, "releases")).thenReturn(ImmutableList.of("/fakepath"));
    List<String> jobs = nexusBuildService.getArtifactPaths("releases", null, nexusConfig, null);
    assertThat(jobs).hasSize(1).containsExactlyInAnyOrder("/fakepath");
  }

  @Test
  public void shouldGetBuilds() {
    when(nexusService.getVersions(nexusConfig, null, BUILD_JOB_NAME, ARTIFACT_GROUP_ID, ARTIFACT_NAME))
        .thenReturn(
            Lists.newArrayList(aBuildDetails().withNumber("3.0").build(), aBuildDetails().withNumber("2.1.2").build()));
    List<BuildDetails> buildDetails =
        nexusBuildService.getBuilds("nexus", nexusArtifactStream.getArtifactStreamAttributes(), nexusConfig, null);
    assertThat(buildDetails).hasSize(2).extracting(BuildDetails::getNumber).containsExactly("3.0", "2.1.2");
  }

  @Test
  public void shouldValidateInvalidUrl() {
    NexusConfig nexusConfig = NexusConfig.builder()
                                  .nexusUrl("BAD_URL")
                                  .username("username")
                                  .password("password".toCharArray())
                                  .accountId(ACCOUNT_ID)
                                  .build();
    try {
      nexusBuildService.validateArtifactServer(nexusConfig);
    } catch (WingsException e) {
      assertThat(e.getMessage()).isEqualTo(ErrorCode.INVALID_ARTIFACT_SERVER.toString());
      assertThat(e.getParams()).isNotEmpty();
      assertThat(e.getParams().get("message")).isEqualTo("Nexus URL must be a valid URL");
    }
  }
}

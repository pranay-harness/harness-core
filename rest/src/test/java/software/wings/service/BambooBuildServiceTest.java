package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_NAME;
import static software.wings.utils.WingsTestConstants.BUILD_JOB_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.BambooConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.artifact.BambooArtifactStream;
import software.wings.beans.config.NexusConfig;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.bamboo.BambooService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.service.intfc.BambooBuildService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;

/**
 * Created by anubhaw on 12/1/16.
 */
public class BambooBuildServiceTest extends WingsBaseTest {
  @Mock private BambooService bambooService;
  @Inject @InjectMocks private BambooBuildService bambooBuildService;

  private static final BambooConfig bambooConfig = BambooConfig.Builder.aBambooConfig()
                                                       .withBambooUrl("http://bamboo")
                                                       .withUsername("username")
                                                       .withPassword("password".toCharArray())
                                                       .build();
  private static final BambooArtifactStream bambooArtifactStream = BambooArtifactStream.Builder.aBambooArtifactStream()
                                                                       .withUuid(ARTIFACT_STREAM_ID)
                                                                       .withAppId(APP_ID)
                                                                       .withSettingId("")
                                                                       .withSourceName(ARTIFACT_STREAM_NAME)
                                                                       .withJobname(BUILD_JOB_NAME)
                                                                       .build();

  @Before
  public void setUp() throws Exception {}

  @Test
  public void shouldGetBuilds() {
    when(bambooService.getBuilds(bambooConfig, BUILD_JOB_NAME, 50))
        .thenReturn(
            Lists.newArrayList(aBuildDetails().withNumber("10").build(), aBuildDetails().withNumber("9").build()));
    List<BuildDetails> builds =
        bambooBuildService.getBuilds(APP_ID, bambooArtifactStream.getArtifactStreamAttributes(), bambooConfig);
    assertThat(builds).hasSize(2).extracting(BuildDetails::getNumber).containsExactly("10", "9");
  }

  @Test
  public void shouldGetPlans() {
    when(bambooService.getPlanKeys(bambooConfig))
        .thenReturn(ImmutableMap.of("PlanAKey", "PlanAName", "PlanBKey", "PlanBName"));
    List<JobDetails> jobs = bambooBuildService.getJobs(bambooConfig, Optional.empty());
    List<String> jobNames = bambooBuildService.extractJobNameFromJobDetails(jobs);
    assertThat(jobNames).hasSize(2).containsExactlyInAnyOrder("PlanAKey", "PlanBKey");
  }

  @Test
  public void shouldGetArtifactPaths() {
    List<String> artifactPaths = bambooBuildService.getArtifactPaths(BUILD_JOB_NAME, null, bambooConfig);
    assertThat(artifactPaths.size()).isEqualTo(0);
  }

  @Test
  public void shouldGetLastSuccessfulBuild() {
    when(bambooService.getLastSuccessfulBuild(bambooConfig, BUILD_JOB_NAME))
        .thenReturn(aBuildDetails().withNumber("10").build());
    BuildDetails lastSuccessfulBuild = bambooBuildService.getLastSuccessfulBuild(
        APP_ID, bambooArtifactStream.getArtifactStreamAttributes(), bambooConfig);
    assertThat(lastSuccessfulBuild.getNumber()).isEqualTo("10");
  }

  @Test
  public void shouldValidateInvalidUrl() {
    BambooConfig bambooConfig = BambooConfig.Builder.aBambooConfig()
                                    .withBambooUrl("BAD_URL")
                                    .withUsername("username")
                                    .withPassword("password".toCharArray())
                                    .build();
    try {
      bambooBuildService.validateArtifactServer(bambooConfig);
    } catch (WingsException e) {
      assertThat(e.getMessage()).isEqualTo(ErrorCode.INVALID_ARTIFACT_SERVER.toString());
      assertThat(e.getParams()).isNotEmpty();
      assertThat(e.getParams().get("message")).isEqualTo("Bamboo URL must be a valid URL");
    }
  }
}

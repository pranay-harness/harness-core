package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;
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
import software.wings.beans.artifact.BambooArtifactStream;
import software.wings.helpers.ext.bamboo.BambooService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BambooBuildService;

import java.util.List;
import java.util.Set;
import javax.inject.Inject;

/**
 * Created by anubhaw on 12/1/16.
 */
public class BambooBuildServiceTest extends WingsBaseTest {
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private BambooService bambooService;
  @Inject @InjectMocks private BambooBuildService bambooBuildService;

  private static final BambooConfig bambooConfig = BambooConfig.Builder.aBambooConfig()
                                                       .withBamboosUrl("http://bamboo")
                                                       .withUsername("username")
                                                       .withPassword("password")
                                                       .build();

  @Before
  public void setUp() throws Exception {
    BambooArtifactStream bambooArtifactStream = BambooArtifactStream.Builder.aBambooArtifactStream()
                                                    .withUuid(ARTIFACT_STREAM_ID)
                                                    .withAppId(APP_ID)
                                                    .withSettingId("")
                                                    .withSourceName(ARTIFACT_STREAM_NAME)
                                                    .withJobname(BUILD_JOB_NAME)
                                                    .withArtifactPathServices(Lists.newArrayList())
                                                    .build();
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(bambooArtifactStream);
  }

  @Test
  public void shouldGetBuilds() {
    when(bambooService.getBuilds(bambooConfig, BUILD_JOB_NAME, 50))
        .thenReturn(
            Lists.newArrayList(aBuildDetails().withNumber("10").build(), aBuildDetails().withNumber("9").build()));
    List<BuildDetails> builds = bambooBuildService.getBuilds(APP_ID, ARTIFACT_STREAM_ID, bambooConfig);
    assertThat(builds).hasSize(2).extracting(BuildDetails::getNumber).containsExactly("10", "9");
  }

  @Test
  public void shouldGetPlans() {
    when(bambooService.getPlanKeys(bambooConfig))
        .thenReturn(ImmutableMap.of("PlanAKey", "PlanAName", "PlanBKey", "PlanBName"));
    Set<String> jobs = bambooBuildService.getJobs(bambooConfig);
    assertThat(jobs).hasSize(2).containsExactlyInAnyOrder("PlanAKey", "PlanBKey");
  }

  @Test
  public void shouldGetArtifactPaths() {
    Set<String> artifactPaths = bambooBuildService.getArtifactPaths(BUILD_JOB_NAME, bambooConfig);
    assertThat(artifactPaths.size()).isEqualTo(0);
  }

  @Test
  public void shouldGetLastSuccessfulBuild() {
    when(bambooService.getLastSuccessfulBuild(bambooConfig, BUILD_JOB_NAME))
        .thenReturn(aBuildDetails().withNumber("10").build());
    BuildDetails lastSuccessfulBuild =
        bambooBuildService.getLastSuccessfulBuild(APP_ID, ARTIFACT_STREAM_ID, bambooConfig);
    assertThat(lastSuccessfulBuild.getNumber()).isEqualTo("10");
  }
}

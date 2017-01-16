package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.BUILD_JOB_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.BambooConfig;
import software.wings.helpers.ext.bamboo.BambooService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.BambooBuildServiceImpl;
import software.wings.service.intfc.BuildService;

import java.util.List;

/**
 * Created by anubhaw on 12/1/16.
 */
public class BambooBuildServiceTest extends WingsBaseTest {
  @Mock private BambooService bambooService;

  @InjectMocks private BuildService<BambooConfig> bambooBuildService = new BambooBuildServiceImpl();

  private static final BambooConfig bambooConfig = BambooConfig.Builder.aBambooConfig()
                                                       .withBamboosUrl("http://bamboo")
                                                       .withUsername("username")
                                                       .withPassword("password")
                                                       .build();

  @Test
  public void shouldGetBuilds() {
    when(bambooService.getBuilds(bambooConfig, BUILD_JOB_NAME, 50))
        .thenReturn(Lists.newArrayList(aBuildDetails().withNumber(10).build(), aBuildDetails().withNumber(9).build()));
    List<BuildDetails> builds = bambooBuildService.getBuilds(APP_ID, BUILD_JOB_NAME, bambooConfig);
    assertThat(builds).hasSize(2).extracting(BuildDetails::getNumber).containsExactly(10, 9);
  }

  @Test
  public void shouldGetPlans() {
    when(bambooService.getPlanKeys(bambooConfig))
        .thenReturn(ImmutableMap.of("PlanAKey", "PlanAName", "PlanBKey", "PlanBName"));
    List<String> jobs = bambooBuildService.getJobs(bambooConfig);
    assertThat(jobs).hasSize(2).containsExactlyInAnyOrder("PlanAKey", "PlanBKey");
  }

  @Test
  public void shouldGetArtifactPaths() {
    List<String> artifactPaths = bambooBuildService.getArtifactPaths(BUILD_JOB_NAME, bambooConfig);
    assertThat(artifactPaths.size()).isEqualTo(0);
  }

  @Test
  public void shouldGetLastSuccessfulBuild() {
    when(bambooService.getLastSuccessfulBuild(bambooConfig, BUILD_JOB_NAME))
        .thenReturn(aBuildDetails().withNumber(10).build());
    BuildDetails lastSuccessfulBuild = bambooBuildService.getLastSuccessfulBuild(APP_ID, BUILD_JOB_NAME, bambooConfig);
    assertThat(lastSuccessfulBuild.getNumber()).isEqualTo(10);
  }
}

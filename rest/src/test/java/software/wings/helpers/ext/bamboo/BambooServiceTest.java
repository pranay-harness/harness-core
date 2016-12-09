package software.wings.helpers.ext.bamboo;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.inject.Inject;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.BambooConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

/**
 * Created by anubhaw on 12/8/16.
 */
public class BambooServiceTest extends WingsBaseTest {
  @Rule public WireMockRule wireMockRule = new WireMockRule(9095);

  @Inject private BambooService bambooService;

  private BambooConfig bambooConfig = BambooConfig.Builder.aBambooConfig()
                                          .withBamboosUrl("http://localhost:9095/rest/api/latest/")
                                          .withUsername("admin")
                                          .withPassword("admin")
                                          .build();

  @Test
  @Ignore
  public void shouldGetJobKeys() {}

  @Test
  public void shouldGetPlanKeys() {
    wireMockRule.stubFor(
        get(urlEqualTo("/rest/api/latest/plan.json?authType=basic"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(
                        "{\"expand\":\"plans\",\"plans\":{\"plan\":[{\"shortName\":\"BAMBOO_PLAN_NAME\",\"key\":\"BAMBOO_PLAN_KEY\"}]}}")
                    .withHeader("Content-Type", "application/json")));
    assertThat(bambooService.getPlanKeys(bambooConfig)).hasSize(1).containsEntry("BAMBOO_PLAN_KEY", "BAMBOO_PLAN_NAME");
  }

  @Test
  @Ignore
  public void shouldGetLastSuccessfulBuild() {}

  @Test
  public void shouldGetBuildsForJob() {
    wireMockRule.stubFor(
        get(urlEqualTo(
                "/rest/api/latest/result/BAMBOO_PLAN_KEY.json?authType=basic&buildState=Successful&expand=results.result&max-result=50"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(
                        "{\"results\":{\"result\":[{\"plan\":{\"vcsRevisionKey\":\"REV_11\",\"buildNumber\":11}},{\"plan\":{\"vcsRevisionKey\":\"REV_12\",\"buildNumber\":12}}]}}")
                    .withHeader("Content-Type", "application/json")));
    List<BuildDetails> bamboo_plan_key = bambooService.getBuilds(bambooConfig, "BAMBOO_PLAN_KEY", 50);
    Assertions.assertThat(bamboo_plan_key)
        .containsExactly(aBuildDetails().withNumber(11).withRevision("REV_11").build(),
            aBuildDetails().withNumber(12).withRevision("REV_12").build());
  }

  @Test
  @Ignore
  public void shouldGetArtifactPath() {}

  @Test
  @Ignore
  public void shouldDownloadArtifact() {}
}

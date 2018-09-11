package software.wings.helpers.ext.bamboo;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.harness.eraro.ErrorCode;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.BambooConfig;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

/**
 * Created by anubhaw on 12/8/16.
 */
@Ignore
public class BambooServiceTest extends WingsBaseTest {
  @Rule public WireMockRule wireMockRule = new WireMockRule(9095);

  private BambooService bambooService = new BambooServiceImpl();

  private BambooConfig bambooConfig = BambooConfig.builder()
                                          .bambooUrl("http://localhost:9095/")
                                          .username("admin")
                                          .password("admin".toCharArray())
                                          .build();

  @Test
  @Ignore
  public void shouldGetJobKeys() {}

  @Test
  @Ignore
  public void shouldGetPlanKeys() {
    wireMockRule.stubFor(
        get(urlEqualTo("/rest/api/latest/plan.json?authType=basic"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(
                        "{\"expand\":\"plans\",\"plans\":{\"plan\":[{\"shortName\":\"BAMBOO_PLAN_NAME\",\"key\":\"BAMBOO_PLAN_KEY\"}]}}")
                    .withHeader("Content-Type", "application/json")));
    assertThat(bambooService.getPlanKeys(bambooConfig, null))
        .hasSize(1)
        .containsEntry("BAMBOO_PLAN_KEY", "BAMBOO_PLAN_NAME");
  }

  @Test
  @Ignore
  public void shouldGetPlanKeysException() {
    wireMockRule.stubFor(get(urlEqualTo("/rest/api/latest/plan.json?authType=basic"))
                             .willReturn(aResponse()
                                             .withStatus(200)
                                             .withFault(Fault.MALFORMED_RESPONSE_CHUNK)
                                             .withHeader("Content-Type", "application/json")));
    assertThatThrownBy(() -> bambooService.getPlanKeys(bambooConfig, null))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCode.UNKNOWN_ERROR.name());
  }

  @Test
  @Ignore
  public void shouldGetLastSuccessfulBuild() {}

  @Test
  @Ignore
  public void shouldGetBuildsForJob() {
    wireMockRule.stubFor(
        get(urlEqualTo(
                "/rest/api/latest/result/BAMBOO_PLAN_KEY.json?authType=basic&buildState=Successful&expand=results.result&max-result=50"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(
                        "{\"results\":{\"result\":[{\"vcsRevisionKey\":\"REV_11\",\"buildNumber\":11}, {\"vcsRevisionKey\":\"REV_12\",\"buildNumber\":12}]}}")
                    .withHeader("Content-Type", "application/json")));
    List<BuildDetails> bamboo_plan_key = bambooService.getBuilds(bambooConfig, null, "BAMBOO_PLAN_KEY", 50);
    assertThat(bamboo_plan_key)
        .containsExactly(aBuildDetails().withNumber("11").withRevision("REV_11").build(),
            aBuildDetails().withNumber("12").withRevision("REV_12").build());
  }

  @Test
  @Ignore
  public void shouldGetBuildsForJobError() {
    wireMockRule.stubFor(get(
        urlEqualTo(
            "/rest/api/latest/result/BAMBOO_PLAN_KEY.json?authType=basic&buildState=Successful&expand=results.result&max-result=50"))
                             .willReturn(aResponse().withStatus(200).withFault(Fault.MALFORMED_RESPONSE_CHUNK)));
    assertThatThrownBy(() -> bambooService.getBuilds(bambooConfig, null, "BAMBOO_PLAN_KEY", 50))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCode.UNKNOWN_ERROR.name());
  }

  @Test
  @Ignore
  public void shouldGetArtifactPath() {}

  @Test
  @Ignore
  public void shouldDownloadArtifact() {}
}

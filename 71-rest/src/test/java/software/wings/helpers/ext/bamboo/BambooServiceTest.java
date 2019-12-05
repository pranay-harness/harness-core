package software.wings.helpers.ext.bamboo;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.harness.rule.OwnerRule.AADITI;
import static io.harness.rule.OwnerRule.ANUBHAW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.MapEntry.entry;
import static org.joor.Reflect.on;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.common.util.concurrent.FakeTimeLimiter;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.harness.category.element.UnitTests;
import io.harness.delegate.exception.ArtifactServerException;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.BambooConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;

/**
 * Created by anubhaw on 12/8/16.
 */
public class BambooServiceTest extends WingsBaseTest {
  @Rule public WireMockRule wireMockRule = new WireMockRule(9095);

  @Mock private EncryptionService encryptionService;
  private BambooService bambooService = new BambooServiceImpl();

  private BambooConfig bambooConfig = BambooConfig.builder()
                                          .bambooUrl("http://localhost:9095/")
                                          .username("admin")
                                          .password("admin".toCharArray())
                                          .build();
  @Before
  public void setupMocks() {
    on(bambooService).set("timeLimiter", new FakeTimeLimiter());
    on(bambooService).set("encryptionService", encryptionService);
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldGetJobKeys() {}

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldGetPlanKeys() {
    assertThat(bambooService.getPlanKeys(bambooConfig, null))
        .contains(entry("TES-PLAN", "plan-1"), entry("TES-PLN10", "plan-10"), entry("TES-PLN100", "plan-100"),
            entry("TES-PLN1000", "plan-1000"), entry("TES-PLN10000", "plan-10000"), entry("TES-PLN1001", "plan-1001"));
  }

  @Test(expected = ArtifactServerException.class)
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldGetPlanKeysExceptionWithInvalidCreds() {
    wireMockRule.stubFor(get(urlEqualTo("/rest/api/latest/plan.json?authType=basic&max-results=1000"))
                             .willReturn(aResponse().withStatus(401).withHeader("Content-Type", "application/json")));
    bambooService.getPlanKeys(bambooConfig, null);
  }

  @Test(expected = ArtifactServerException.class)
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldGetPlanKeysException() {
    wireMockRule.stubFor(get(urlEqualTo("/rest/api/latest/plan.json?authType=basic&max-results=1000"))
                             .willReturn(aResponse().withStatus(500).withHeader("Content-Type", "application/json")));
    bambooService.getPlanKeys(bambooConfig, null);
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldGetLastSuccessfulBuild() {
    BuildDetails buildDetails = bambooService.getLastSuccessfulBuild(bambooConfig, null, "TOD-TODIR");
    assertThat(buildDetails).isNotNull();
    assertThat(buildDetails.getNumber()).isEqualTo("104");
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldNotGetLastSuccessfulBuild() {
    BuildDetails buildDetails = bambooService.getLastSuccessfulBuild(bambooConfig, null, "TOD-TOD");
    assertThat(buildDetails).isNull();
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
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
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
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
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldGetArtifactPath() {}

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldDownloadArtifact() {}
}

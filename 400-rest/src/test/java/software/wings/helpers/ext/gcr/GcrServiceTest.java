package software.wings.helpers.ext.gcr;

import static io.harness.rule.OwnerRule.DEEPAK_PUTHRAYA;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.artifacts.gcr.beans.GcpInternalConfig;
import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.service.mappers.artifact.GcpConfigToInternalMapper;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GcrServiceTest extends WingsBaseTest {
  GcrServiceImpl gcrService = spy(new GcrServiceImpl());
  @Rule public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(9881));
  private static final String url = "localhost:9881";
  String basicAuthHeader = "auth";
  GcpInternalConfig gcpInternalConfig = GcpConfigToInternalMapper.toGcpInternalConfig(url, basicAuthHeader);

  @Before
  public void setUp() {
    wireMockRule.stubFor(WireMock.get(WireMock.urlEqualTo("/v2/someImage/tags/list"))
                             .withHeader("Authorization", equalTo("auth"))
                             .willReturn(aResponse().withStatus(200).withBody(
                                 "{\"name\":\"someImage\",\"tags\":[\"v1\",\"v2\",\"latest\"]}")));
    when(gcrService.getUrl(anyString())).thenReturn("http://localhost:9881/");
  }

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void shouldGetBuilds() {
    List<BuildDetailsInternal> actual = gcrService.getBuilds(gcpInternalConfig, "someImage", 100);
    assertThat(actual).hasSize(3);
    assertThat(actual.stream().map(BuildDetailsInternal::getNumber).collect(Collectors.toList()))
        .isEqualTo(Lists.newArrayList("latest", "v1", "v2"));

    gcrService.getBuilds(GcpConfigToInternalMapper.toGcpInternalConfig(url, basicAuthHeader), "someImage", 100);
    assertThatThrownBy(() -> gcrService.getBuilds(gcpInternalConfig, "doesNotExist", 100))
        .isInstanceOf(WingsException.class);
  }

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void testVerifyImageName() {
    assertThat(gcrService.verifyImageName(gcpInternalConfig, "someImage")).isTrue();
    assertThatThrownBy(() -> gcrService.verifyImageName(gcpInternalConfig, "doesNotExist"))
        .isInstanceOf(WingsException.class);
  }

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void testValidateCredentials() {
    assertThat(gcrService.validateCredentials(gcpInternalConfig, "someImage")).isTrue();
    assertThat(gcrService.validateCredentials(gcpInternalConfig, "doesNotExist")).isFalse();
  }
}

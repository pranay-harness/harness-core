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

import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.GcpConfig;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.GcpHelperService;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

public class GcrServiceTest extends WingsBaseTest {
  GcpHelperService gcpHelperService = Mockito.mock(GcpHelperService.class);
  GcrServiceImpl gcrService = spy(new GcrServiceImpl(gcpHelperService));
  @Rule public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(9881));
  private static final String url = "localhost:9881";

  @Before
  public void setUp() {
    Mockito.when(gcpHelperService.getDefaultCredentialsAccessToken(TaskType.GCP_TASK)).thenReturn("auth");
    wireMockRule.stubFor(WireMock.get(WireMock.urlEqualTo("/v2/someImage/tags/list"))
                             .withHeader("Authorization", equalTo("auth"))
                             .willReturn(aResponse().withStatus(200).withBody(
                                 "{\"name\":\"someImage\",\"tags\":[\"v1\",\"v2\",\"latest\"]}")));
    when(gcrService.getUrl(anyString())).thenReturn("http://localhost:9881/");
  }

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void shouldGetBuilds() throws IOException {
    List<BuildDetails> actual = gcrService.getBuilds(GcpConfig.builder().useDelegate(true).build(), null,
        ArtifactStreamAttributes.builder().imageName("someImage").registryHostName(url).build(), 100);
    assertThat(actual).hasSize(3);
    assertThat(actual.stream().map(BuildDetails::getNumber).collect(Collectors.toList()))
        .isEqualTo(Lists.newArrayList("latest", "v1", "v2"));

    assertThatThrownBy(
        ()
            -> gcrService.getBuilds(GcpConfig.builder().useDelegate(true).build(), null,
                ArtifactStreamAttributes.builder().imageName("doesNotExist").registryHostName(url).build(), 100))
        .isInstanceOf(WingsException.class);
  }

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void testVerifyImageName() throws IOException {
    assertThat(gcrService.verifyImageName(GcpConfig.builder().useDelegate(true).build(), null,
                   ArtifactStreamAttributes.builder().imageName("someImage").registryHostName(url).build()))
        .isTrue();
    assertThatThrownBy(
        ()
            -> gcrService.verifyImageName(GcpConfig.builder().useDelegate(true).build(), null,
                ArtifactStreamAttributes.builder().imageName("doesNotExist").registryHostName(url).build()))
        .isInstanceOf(WingsException.class);
  }

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void testValidateCredentials() throws IOException {
    assertThat(gcrService.validateCredentials(GcpConfig.builder().useDelegate(true).build(), null,
                   ArtifactStreamAttributes.builder().imageName("someImage").registryHostName(url).build()))
        .isTrue();
    assertThat(gcrService.validateCredentials(GcpConfig.builder().useDelegate(true).build(), null,
                   ArtifactStreamAttributes.builder().imageName("doesNotExist").registryHostName(url).build()))
        .isFalse();
  }
}

package software.wings.helpers.ext.docker;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.harness.exception.ExceptionUtils.getMessage;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.ROHITKARELIA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;

import com.google.inject.Inject;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.harness.artifacts.docker.beans.DockerInternalConfig;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.serializer.JsonUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.WingsBaseTest;
import software.wings.exception.InvalidArtifactServerException;

public class DockerRegistryServiceImplTest extends WingsBaseTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public WireMockRule wireMockRule = new WireMockRule(9883);
  @Inject @InjectMocks DockerRegistryService dockerRegistryService;

  private static final String url = "http://localhost:9883/";
  private static DockerInternalConfig dockerConfig =
      DockerInternalConfig.builder().dockerRegistryUrl(url).username("username").password("password").build();

  @Test(expected = InvalidArtifactServerException.class)
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testValidateCredentialForIOException() {
    wireMockRule.stubFor(get(urlEqualTo("/v2/"))
                             .willReturn(aResponse().withStatus(401).withHeader("Www-Authenticate",
                                 "Bearer realm=\"https://localhost:9883/service/token\",service=\"harbor-registry\"")));
    dockerRegistryService.validateCredentials(dockerConfig);
  }

  @Test(expected = InvalidArtifactServerException.class)
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testValidateCredentialForIOException2() {
    wireMockRule.stubFor(get(urlEqualTo("/v2"))
                             .willReturn(aResponse().withStatus(401).withHeader("Www-Authenticate",
                                 "Bearer realm=\"https://localhost:9883/service/token\",service=\"harbor-registry\"")));
    // http://localhost:9883/service/token?service=harbor-registry&scope=somevalue
    wireMockRule.stubFor(get(urlEqualTo("/service/token?service=harbor-registry&scope=somevalue"))
                             .willReturn(aResponse().withBody(JsonUtils.asJson(getDockerRegistryToken()))));
    dockerRegistryService.validateCredentials(dockerConfig);
  }

  @Test(expected = InvalidArtifactServerException.class)
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testValidateCredentialForIOExceptionForGetAPIEndingWithForwardSlash() {
    wireMockRule.stubFor(get(urlEqualTo("/v2/")).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
    dockerRegistryService.validateCredentials(dockerConfig);
  }

  @Test(expected = InvalidArtifactServerException.class)
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testValidateCredentialIOExceptionForAllgetapiVersionCalls() throws Exception {
    wireMockRule.stubFor(get(urlEqualTo("/v2")).willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)));
    wireMockRule.stubFor(
        get(urlEqualTo("/v2/"))
            .willReturn(aResponse().withStatus(401).withHeader("Www-Authenticate",
                "Bearer realm=\"http://localhost:9883/service/token\",service=\"harbor-registry\",scope=\"somevalue\"")));

    // http://localhost:9883/service/token?service=harbor-registry&scope=somevalue
    wireMockRule.stubFor(get(urlEqualTo("/service/token?service=harbor-registry&scope=somevalue"))
                             .willReturn(aResponse().withBody(JsonUtils.asJson(getDockerRegistryToken()))));
    dockerRegistryService.validateCredentials(dockerConfig);
  }

  @NotNull
  private DockerRegistryServiceImpl.DockerRegistryToken getDockerRegistryToken() {
    DockerRegistryServiceImpl.DockerRegistryToken dockerRegistryToken =
        new DockerRegistryServiceImpl.DockerRegistryToken();
    dockerRegistryToken.setToken("dockerregistryToken");
    return dockerRegistryToken;
  }

  @Test()
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testValidateCredentialForMissingPassword() {
    try {
      DockerInternalConfig dockerInternalConfig =
          DockerInternalConfig.builder().dockerRegistryUrl(url).username("username").build();
      dockerRegistryService.validateCredentials(dockerInternalConfig);
      fail("Should not reach here");
    } catch (Exception ex) {
      assertThat(getMessage(ex)).isEqualTo("Password is a required field along with Username");
    }
  }

  @Test()
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetBuildDetails() {
    try {
      dockerRegistryService.getBuilds(dockerConfig, "image", 10);
      fail("Should not reach here");
    } catch (Exception ex) {
      assertThat(getMessage(ex)).isEqualTo("Bad Request");
    }
  }

  @Test()
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testIsSuccessfulNullResponse() {
    try {
      DockerRegistryServiceImpl.isSuccessful(null);
      fail("Should not reach here");
    } catch (Exception ex) {
      assertThat(getMessage(ex)).isEqualTo("Null response found");
    }
  }

  @Test()
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testIsSuccessfulErrorCode500() {
    try {
      dockerRegistryService.getBuilds(dockerConfig, "image_500", 10);
      fail("Should not reach here");
    } catch (Exception ex) {
      assertThat(getMessage(ex)).isEqualTo("Internal Server Error");
    }
  }
}

package io.harness.artifacts.docker.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.harness.exception.ExceptionUtils.getMessage;
import static io.harness.logging.LoggingInitializer.initializeLogging;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.ARCHIT;
import static io.harness.rule.OwnerRule.ROHITKARELIA;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.harness.CategoryTest;
import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.artifacts.docker.DockerRegistryRestClient;
import io.harness.artifacts.docker.beans.DockerInternalConfig;
import io.harness.artifacts.docker.client.DockerRestClientFactory;
import io.harness.artifacts.docker.client.DockerRestClientFactoryImpl;
import io.harness.artifacts.docker.service.DockerRegistryServiceImpl.DockerImageTagResponse;
import io.harness.artifacts.docker.service.DockerRegistryServiceImpl.DockerRegistryToken;
import io.harness.category.element.UnitTests;
import io.harness.exception.ArtifactServerException;
import io.harness.exception.InvalidArtifactServerException;
import io.harness.network.Http;
import io.harness.rule.Owner;
import io.harness.serializer.JsonUtils;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DockerRegistryServiceImplTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule
  public WireMockRule wireMockRule = new WireMockRule(
      WireMockConfiguration.wireMockConfig().usingFilesUnderDirectory("15-api-services/src/test/resources").port(9883));
  @Mock private DockerRestClientFactory dockerRestClientFactory;
  @Mock private DockerRegistryUtils dockerRegistryUtils;
  @InjectMocks DockerRegistryServiceImpl dockerRegistryService;

  private static String url;
  private static DockerInternalConfig dockerConfig;
  private static DockerRegistryToken dockerRegistryToken;
  private static DockerRegistryRestClient dockerRegistryRestClient;
  private static DockerImageTagResponse dockerImageTagResponse;
  private static DockerImageTagResponse dockerImageTagResponsePaginated;

  @BeforeClass
  public static void beforeClass() {
    dockerRegistryToken = new DockerRegistryToken();
    dockerRegistryToken.setToken("dockerRegistryToken");

    url = "http://localhost:9883/";
    dockerConfig =
        DockerInternalConfig.builder().dockerRegistryUrl(url).username("username").password("password").build();
    dockerRegistryRestClient = new DockerRestClientFactoryImpl().getDockerRegistryRestClient(dockerConfig);

    dockerImageTagResponse = new DockerImageTagResponse();
    dockerImageTagResponse.setName("DOCKER_IMAGE");
    List<String> tags = Arrays.asList("tag1", "tag2");
    dockerImageTagResponse.setTags(tags);

    dockerImageTagResponsePaginated = new DockerImageTagResponse();
    dockerImageTagResponsePaginated.setName("Docker_Image_Paginated");
    tags = Arrays.asList("tag3", "tag4");
    dockerImageTagResponsePaginated.setTags(tags);

    initializeLogging();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testValidateCredentialForIOException() {
    wireMockRule.stubFor(get(urlEqualTo("/v2/"))
                             .willReturn(aResponse().withStatus(401).withHeader("Www-Authenticate",
                                 "Bearer realm=\"https://localhost:9883/service/token\",service=\"harbor-registry\"")));
    doReturn(dockerRegistryRestClient).when(dockerRestClientFactory).getDockerRegistryRestClient(dockerConfig);
    assertThatThrownBy(() -> dockerRegistryService.validateCredentials(dockerConfig))
        .isInstanceOf(InvalidArtifactServerException.class);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testValidateCredentialForIOException2() {
    doReturn(dockerRegistryRestClient).when(dockerRestClientFactory).getDockerRegistryRestClient(dockerConfig);
    wireMockRule.stubFor(
        get(urlEqualTo("/v2"))
            .willReturn(aResponse().withStatus(401).withHeader("Www-Authenticate",
                "Bearer realm=\"http://localhost:9883/service/token\",service=\"harbor-registry\",scope=\"somevalue\"")));
    // http://localhost:9883/service/token?service=harbor-registry&scope=somevalue
    wireMockRule.stubFor(get(urlEqualTo("/service/token?service=harbor-registry&scope=somevalue"))
                             .willReturn(aResponse().withBody(JsonUtils.asJson(dockerRegistryToken))));
    assertThatThrownBy(() -> dockerRegistryService.validateCredentials(dockerConfig))
        .isInstanceOf(InvalidArtifactServerException.class);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testValidateCredentialForIOExceptionForGetAPIEndingWithForwardSlash() {
    doReturn(dockerRegistryRestClient).when(dockerRestClientFactory).getDockerRegistryRestClient(dockerConfig);
    wireMockRule.stubFor(get(urlEqualTo("/v2/")).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
    assertThatThrownBy(() -> dockerRegistryService.validateCredentials(dockerConfig))
        .isInstanceOf(InvalidArtifactServerException.class);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testValidateCredentialIOExceptionForAllgetapiVersionCalls() {
    doReturn(dockerRegistryRestClient).when(dockerRestClientFactory).getDockerRegistryRestClient(dockerConfig);
    wireMockRule.stubFor(get(urlEqualTo("/v2")).willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)));
    wireMockRule.stubFor(
        get(urlEqualTo("/v2/"))
            .willReturn(aResponse().withStatus(401).withHeader("Www-Authenticate",
                "Bearer realm=\"http://localhost:9883/service/token\",service=\"harbor-registry\",scope=\"somevalue\"")));

    // http://localhost:9883/service/token?service=harbor-registry&scope=somevalue
    wireMockRule.stubFor(get(urlEqualTo("/service/token?service=harbor-registry&scope=somevalue"))
                             .willReturn(aResponse().withBody(JsonUtils.asJson(dockerRegistryToken))));
    assertThatThrownBy(() -> dockerRegistryService.validateCredentials(dockerConfig))
        .isInstanceOf(InvalidArtifactServerException.class);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testValidateCredentialForMissingPassword() {
    try {
      DockerInternalConfig dockerInternalConfig =
          DockerInternalConfig.builder().dockerRegistryUrl(url).username("username").build();
      doReturn(dockerRegistryRestClient)
          .when(dockerRestClientFactory)
          .getDockerRegistryRestClient(dockerInternalConfig);
      dockerRegistryService.validateCredentials(dockerInternalConfig);
      fail("Should not reach here");
    } catch (Exception exception) {
      assertThat(getMessage(exception)).isEqualTo("Password is a required field along with Username");
    }
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetBuildDetailsWithException() {
    try {
      doReturn(dockerRegistryRestClient).when(dockerRestClientFactory).getDockerRegistryRestClient(dockerConfig);
      dockerRegistryService.getBuilds(dockerConfig, "image", 10);
      fail("Should not reach here");
    } catch (Exception ex) {
      assertThat(getMessage(ex)).isEqualTo("Bad Request");
    }
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetBuildDetailsInvalidCredentialsException() {
    try {
      doReturn(dockerRegistryRestClient).when(dockerRestClientFactory).getDockerRegistryRestClient(dockerConfig);

      wireMockRule.stubFor(
          get(urlEqualTo("/v2/image/tags/list"))
              .willReturn(aResponse().withStatus(401).withHeader("Www-Authenticate",
                  "Bearer realm=\"http://localhost:9883/service/token\",service=\"harbor-registry\",scope=\"somevalue\"")));

      // http://localhost:9883/service/token?service=harbor-registry&scope=somevalue
      wireMockRule.stubFor(get(urlEqualTo("/service/token?service=harbor-registry&scope=somevalue"))
                               .willReturn(aResponse().withBody(JsonUtils.asJson(dockerRegistryToken))));

      dockerRegistryService.getBuilds(dockerConfig, "image", 10);
      fail("Should not reach here");
    } catch (Exception ex) {
      assertThat(ex).isInstanceOf(ArtifactServerException.class);
      assertThat(getMessage(ex)).isEqualTo("Invalid credentials");
    }
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetBuildDetails() {
    doReturn(dockerRegistryRestClient).when(dockerRestClientFactory).getDockerRegistryRestClient(dockerConfig);

    wireMockRule.stubFor(
        get(urlEqualTo("/v2/image/tags/list"))
            .willReturn(aResponse().withStatus(200).withBody(JsonUtils.asJson(dockerImageTagResponse))));
    List<BuildDetailsInternal> builds = dockerRegistryService.getBuilds(dockerConfig, "image", 10);
    assertThat(builds).isNotNull();
    assertThat(builds.size()).isEqualTo(2);
    assertThat(builds.get(0).getNumber()).isEqualTo("tag1");
    assertThat(builds.get(0).getBuildUrl()).isEqualTo(url + "image/tags/tag1");
    assertThat(builds.get(0).getMetadata().get("tag")).isEqualTo("tag1");
    assertThat(builds.get(0).getMetadata().get("image")).isEqualTo(Http.getDomainWithPort(url) + "/image:tag1");
    assertThat(builds.get(1).getNumber()).isEqualTo("tag2");
    assertThat(builds.get(1).getBuildUrl()).isEqualTo(url + "image/tags/tag2");
    assertThat(builds.get(1).getMetadata().get("tag")).isEqualTo("tag2");
    assertThat(builds.get(1).getMetadata().get("image")).isEqualTo(Http.getDomainWithPort(url) + "/image:tag2");
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetBuildDetailsPaginatedResponse() {
    doReturn(dockerRegistryRestClient).when(dockerRestClientFactory).getDockerRegistryRestClient(dockerConfig);

    wireMockRule.stubFor(
        get(urlEqualTo("/v2/image/tags/list"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(JsonUtils.asJson(dockerImageTagResponse))
                    .withHeader("link", "</v2/tags/list?next_page=gAAAAABbuZsLNl9W6tAycol&n=2>; rel=\"next\"")));

    wireMockRule.stubFor(
        get(urlEqualTo("/v2/image/tags/list?next_page=gAAAAABbuZsLNl9W6tAycol&n=2"))
            .willReturn(aResponse().withStatus(200).withBody(JsonUtils.asJson(dockerImageTagResponsePaginated))));

    List<BuildDetailsInternal> builds = dockerRegistryService.getBuilds(dockerConfig, "image", 10);
    assertThat(builds).isNotNull();
    assertThat(builds.get(0).getNumber()).isEqualTo("tag1");
    assertThat(builds.get(0).getBuildUrl()).isEqualTo(url + "image/tags/tag1");
    assertThat(builds.get(0).getMetadata().get("tag")).isEqualTo("tag1");
    assertThat(builds.get(0).getMetadata().get("image")).isEqualTo(Http.getDomainWithPort(url) + "/image:tag1");
    assertThat(builds.get(1).getNumber()).isEqualTo("tag2");
    assertThat(builds.get(1).getBuildUrl()).isEqualTo(url + "image/tags/tag2");
    assertThat(builds.get(1).getMetadata().get("tag")).isEqualTo("tag2");
    assertThat(builds.get(1).getMetadata().get("image")).isEqualTo(Http.getDomainWithPort(url) + "/image:tag2");

    assertThat(builds.get(2).getNumber()).isEqualTo("tag3");
    assertThat(builds.get(2).getBuildUrl()).isEqualTo(url + "image/tags/tag3");
    assertThat(builds.get(2).getMetadata().get("tag")).isEqualTo("tag3");
    assertThat(builds.get(2).getMetadata().get("image")).isEqualTo(Http.getDomainWithPort(url) + "/image:tag3");
    assertThat(builds.get(3).getNumber()).isEqualTo("tag4");
    assertThat(builds.get(3).getBuildUrl()).isEqualTo(url + "image/tags/tag4");
    assertThat(builds.get(3).getMetadata().get("tag")).isEqualTo("tag4");
    assertThat(builds.get(3).getMetadata().get("image")).isEqualTo(Http.getDomainWithPort(url) + "/image:tag4");
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testVerifyImageName() {
    doReturn(dockerRegistryRestClient).when(dockerRestClientFactory).getDockerRegistryRestClient(dockerConfig);

    wireMockRule.stubFor(
        get(urlEqualTo("/v2/image/tags/list"))
            .willReturn(aResponse().withStatus(200).withBody(JsonUtils.asJson(dockerImageTagResponse))));

    boolean image = dockerRegistryService.verifyImageName(dockerConfig, "image");
    assertThat(image).isTrue();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetLabels() {
    doReturn(dockerRegistryRestClient).when(dockerRestClientFactory).getDockerRegistryRestClient(dockerConfig);
    Map<String, String> labelMap = new HashMap<>();
    labelMap.put("abc", "abc");
    Map<String, String> anotherLabelMap = new HashMap<>();
    anotherLabelMap.put("abc1", "abc1");
    List<Map<String, String>> labelsMapList = Arrays.asList(labelMap, anotherLabelMap);

    doReturn(dockerRegistryRestClient).when(dockerRestClientFactory).getDockerRegistryRestClient(dockerConfig);
    doReturn(labelsMapList).when(dockerRegistryUtils).getLabels(any(), any(), any(), any(), any());

    List<Map<String, String>> resultLabelsMap =
        dockerRegistryService.getLabels(dockerConfig, "image", asList("abc", "latest"));
    assertThat(resultLabelsMap).isNotNull();
    assertThat(resultLabelsMap.size()).isEqualTo(2);
    assertThat(resultLabelsMap.get(0).get("abc")).isEqualTo("abc");
    assertThat(resultLabelsMap.get(1).get("abc1")).isEqualTo("abc1");
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetLastSuccessfulBuild() {
    BuildDetailsInternal buildDetailsInternal = dockerRegistryService.getLastSuccessfulBuild(dockerConfig, "image");
    assertThat(buildDetailsInternal).isNull();
  }

  @Test
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

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testIsSuccessfulErrorCode500() {
    try {
      doReturn(dockerRegistryRestClient).when(dockerRestClientFactory).getDockerRegistryRestClient(dockerConfig);
      dockerRegistryService.getBuilds(dockerConfig, "image_500", 10);
      fail("Should not reach here");
    } catch (Exception ex) {
      assertThat(getMessage(ex)).isEqualTo("Internal Server Error");
    }
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldParseLink() {
    String link =
        "</v2/myAccount/myfirstrepo/tags/list?next_page=gAAAAABbuZsLNl9W6tAycol_oLvcYeti2w53XnoV3FYyFBkd-TQV3OBiWNJLqp2m8isy3SWusAqA4Y32dHJ7tGi0br18kXEt6nTW306QUFexaXrAGq8KeSc%3D&n=25>; rel=\"next\"";
    String parsedLink = DockerRegistryServiceImpl.parseLink(link);
    assertThat(parsedLink).isNotEmpty();
    assertThat(parsedLink)
        .isEqualTo(
            "v2/myAccount/myfirstrepo/tags/list?next_page=gAAAAABbuZsLNl9W6tAycol_oLvcYeti2w53XnoV3FYyFBkd-TQV3OBiWNJLqp2m8isy3SWusAqA4Y32dHJ7tGi0br18kXEt6nTW306QUFexaXrAGq8KeSc%3D&n=25");
  }
}

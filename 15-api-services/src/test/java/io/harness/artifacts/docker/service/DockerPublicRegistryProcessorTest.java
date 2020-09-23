package io.harness.artifacts.docker.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.harness.exception.ExceptionUtils.getMessage;
import static io.harness.logging.LoggingInitializer.initializeLogging;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.ARCHIT;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.harness.CategoryTest;
import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.artifacts.docker.DockerRegistryRestClient;
import io.harness.artifacts.docker.beans.DockerInternalConfig;
import io.harness.artifacts.docker.beans.DockerPublicImageTagResponse;
import io.harness.artifacts.docker.client.DockerRestClientFactory;
import io.harness.artifacts.docker.client.DockerRestClientFactoryImpl;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArtifactServerException;
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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DockerPublicRegistryProcessorTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule
  public WireMockRule wireMockRule = new WireMockRule(
      WireMockConfiguration.wireMockConfig().usingFilesUnderDirectory("15-api-services/src/test/resources").port(9882));

  @Mock private DockerRestClientFactory dockerRestClientFactory;
  @Mock private DockerRegistryUtils dockerRegistryUtils;
  @Mock private DockerPublicImageTagResponse dockerPublicImageTagResponse;
  @Mock private DockerPublicImageTagResponse.Result result;
  @InjectMocks DockerPublicRegistryProcessor dockerPublicRegistryProcessor;

  private static DockerInternalConfig dockerConfig;
  private static DockerRegistryRestClient dockerRegistryRestClient;

  @BeforeClass
  public static void beforeClass() {
    String url = "http://localhost:9882/";
    dockerConfig =
        DockerInternalConfig.builder().dockerRegistryUrl(url).username("username").password("password").build();
    dockerRegistryRestClient = new DockerRestClientFactoryImpl().getDockerRegistryRestClient(dockerConfig);
    initializeLogging();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testPaginate() throws Exception {
    List<BuildDetailsInternal> images = dockerPublicRegistryProcessor.paginate(null, dockerConfig, "image", null, 10);
    assertThat(images).isEmpty();

    when(result.getName()).thenReturn("1");
    when(dockerPublicImageTagResponse.getResults()).thenReturn(asList(result));
    when(dockerPublicImageTagResponse.getNext()).thenReturn("http://localhost:9882/v2/");
    images = dockerPublicRegistryProcessor.paginate(dockerPublicImageTagResponse, dockerConfig, "image", null, 10);
    assertThat(images).isNotEmpty();
    assertThat(images.get(0).getUiDisplayName()).isEqualTo("Tag# 1");
    assertThat(images.get(0).getBuildUrl()).isEqualTo("http://localhost:9882/image/tags/1");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetBuildsWithUnAuthorisedException() throws IOException {
    try {
      doReturn(dockerRegistryRestClient).when(dockerRestClientFactory).getDockerRegistryRestClient(dockerConfig);
      dockerPublicRegistryProcessor.getBuilds(dockerConfig, "image", 10);
      fail("Should not reach here");
    } catch (InvalidArtifactServerException ex) {
      assertThat(getMessage(ex)).isEqualTo("Invalid Docker Registry credentials");
    }
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetBuildsWithNotFoundException() throws IOException {
    try {
      doReturn(dockerRegistryRestClient).when(dockerRestClientFactory).getDockerRegistryRestClient(dockerConfig);
      dockerPublicRegistryProcessor.getBuilds(dockerConfig, "image-1", 10);
      fail("Should not reach here");
    } catch (InvalidArtifactServerException ex) {
      assertThat(getMessage(ex)).isEqualTo("Not Found");
    }
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetBuildsPaginatedException() throws IOException {
    try {
      doReturn(dockerRegistryRestClient).when(dockerRestClientFactory).getDockerRegistryRestClient(dockerConfig);
      dockerPublicRegistryProcessor.getBuilds(dockerConfig, "image-paginated", 1);
      fail("Should not reach here");
    } catch (InvalidArtifactServerException ex) {
      assertThat(getMessage(ex)).isEqualTo("Bad Request");
    }
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
        dockerPublicRegistryProcessor.getLabels(dockerConfig, "image", asList("abc", "latest"));
    assertThat(resultLabelsMap).isNotNull();
    assertThat(resultLabelsMap.size()).isEqualTo(2);
    assertThat(resultLabelsMap.get(0).get("abc")).isEqualTo("abc");
    assertThat(resultLabelsMap.get(1).get("abc1")).isEqualTo("abc1");
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testVerifyImageName() {
    doReturn(dockerRegistryRestClient).when(dockerRestClientFactory).getDockerRegistryRestClient(dockerConfig);

    DockerPublicImageTagResponse dockerPublicImageTagResponse = new DockerPublicImageTagResponse();

    wireMockRule.stubFor(
        get(urlEqualTo("/v2/repositories/image/tags?page_size=1"))
            .willReturn(aResponse().withStatus(200).withBody(JsonUtils.asJson(dockerPublicImageTagResponse))));

    boolean image = dockerPublicRegistryProcessor.verifyImageName(dockerConfig, "image");
    assertThat(image).isTrue();
  }
}

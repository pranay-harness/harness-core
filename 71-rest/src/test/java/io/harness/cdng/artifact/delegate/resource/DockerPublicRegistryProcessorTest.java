package io.harness.cdng.artifact.delegate.resource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.inject.Inject;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.artifact.bean.ArtifactAttributes;
import io.harness.cdng.artifact.bean.DockerArtifactAttributes;
import io.harness.cdng.artifact.bean.connector.DockerhubConnectorConfig;
import io.harness.cdng.artifact.delegate.beans.DockerPublicImageTagResponse;
import io.harness.rule.Owner;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.exception.InvalidArtifactServerException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DockerPublicRegistryProcessorTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public WireMockRule wireMockRule = new WireMockRule(9551);

  @Mock private DockerRegistryRestClient dockerRegistryRestClient;
  @Mock private DockerPublicImageTagResponse.Result result;
  @Mock private DockerPublicImageTagResponse dockerResponse;
  @Mock private DockerPublicImageTagResponse dockerResponse2;
  @Spy @Inject @InjectMocks DockerPublicRegistryProcessor dockerPublicRegistryProcessor;

  private static DockerhubConnectorConfig connectorConfig;

  @BeforeClass
  public static void beforeClass() {
    String url = "http://localhost:9551/";
    connectorConfig = DockerhubConnectorConfig.builder().registryUrl(url).identifier("CONNECTOR_CONFIG").build();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetLastSuccessfulBuild() throws IOException {
    Call<DockerPublicImageTagResponse.Result> requestCall = mock(Call.class);
    Response<DockerPublicImageTagResponse.Result> response = Response.success(result);
    doReturn("tag").when(result).getName();
    doReturn(response).when(requestCall).execute();
    doReturn(dockerRegistryRestClient).when(dockerPublicRegistryProcessor).getDockerRegistryRestClient(connectorConfig);
    doReturn(requestCall).when(dockerRegistryRestClient).getPublicImageTag("image", "tag");
    ArtifactAttributes lastSuccessfulBuild =
        dockerPublicRegistryProcessor.getLastSuccessfulBuild(connectorConfig, "image", "tag");

    assertThat(lastSuccessfulBuild).isInstanceOf(DockerArtifactAttributes.class);
    DockerArtifactAttributes artifactAttributes = (DockerArtifactAttributes) lastSuccessfulBuild;
    assertThat(artifactAttributes.getImagePath()).isEqualTo("image");
    assertThat(artifactAttributes.getTag()).isEqualTo(result.getName());
    assertThat(artifactAttributes.getDockerHubConnector()).isEqualTo(connectorConfig.getIdentifier());
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testProcessImageReturnsNull() {
    DockerArtifactAttributes artifactAttributes =
        dockerPublicRegistryProcessor.processSingleResultResponse(null, "image", connectorConfig);
    assertThat(artifactAttributes).isNull();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testProcessImagesReturnsEmptyList() {
    List<DockerArtifactAttributes> artifactAttributes =
        dockerPublicRegistryProcessor.processPageResponse(null, connectorConfig, "image");
    assertThat(artifactAttributes).isEmpty();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetLatestSuccessfulBuildWithException() {
    // http://localhost:9551/v2/repositories/image/tags/tag
    wireMockRule.stubFor(get(urlEqualTo("/v2/repositories/image/tags/tag")).willReturn(aResponse().withStatus(400)));
    assertThatThrownBy(() -> dockerPublicRegistryProcessor.getLastSuccessfulBuild(connectorConfig, "image", "tag"))
        .isInstanceOf(InvalidArtifactServerException.class);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetLastSuccessfulBuildFromRegex() throws IOException {
    Call<DockerPublicImageTagResponse> requestCall = mock(Call.class);
    Response<DockerPublicImageTagResponse> response = Response.success(dockerResponse);
    List<DockerPublicImageTagResponse.Result> results = Arrays.asList(result);
    doReturn(results).when(dockerResponse).getResults();
    doReturn("tagRegexNew").when(result).getName();
    doReturn(response).when(requestCall).execute();
    doReturn(dockerRegistryRestClient).when(dockerPublicRegistryProcessor).getDockerRegistryRestClient(connectorConfig);
    doReturn(requestCall)
        .when(dockerRegistryRestClient)
        .listPublicImageTags("image", null, DockerRegistryService.MAX_NO_OF_TAGS_PER_PUBLIC_IMAGE);
    ArtifactAttributes lastSuccessfulBuild =
        dockerPublicRegistryProcessor.getLastSuccessfulBuildFromRegex(connectorConfig, "image", "tagRegex");

    assertThat(lastSuccessfulBuild).isInstanceOf(DockerArtifactAttributes.class);
    DockerArtifactAttributes artifactAttributes = (DockerArtifactAttributes) lastSuccessfulBuild;
    assertThat(artifactAttributes.getImagePath()).isEqualTo("image");
    assertThat(artifactAttributes.getTag()).isEqualTo(result.getName());
    assertThat(artifactAttributes.getDockerHubConnector()).isEqualTo(connectorConfig.getIdentifier());
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testPaginate() throws IOException {
    List<DockerArtifactAttributes> images =
        dockerPublicRegistryProcessor.paginate(null, connectorConfig, "image", null);
    assertThat(images).isEmpty();

    Call<DockerPublicImageTagResponse> requestCall = mock(Call.class);
    Response<DockerPublicImageTagResponse> response = Response.success(dockerResponse);
    List<DockerPublicImageTagResponse.Result> results = Arrays.asList(result);
    doReturn(results).when(dockerResponse).getResults();
    doReturn("tag").when(result).getName();
    doReturn(response).when(requestCall).execute();
    doReturn(requestCall)
        .when(dockerRegistryRestClient)
        .listPublicImageTags("image", null, DockerRegistryService.MAX_NO_OF_TAGS_PER_PUBLIC_IMAGE);
    doReturn(connectorConfig.getRegistryUrl() + "?page=2").when(dockerResponse).getNext();

    Call<DockerPublicImageTagResponse> requestCall2 = mock(Call.class);
    Response<DockerPublicImageTagResponse> response2 = Response.success(dockerResponse2);
    doReturn(Collections.EMPTY_LIST).when(dockerResponse2).getResults();
    doReturn(response2).when(requestCall2).execute();
    doReturn(requestCall2)
        .when(dockerRegistryRestClient)
        .listPublicImageTags("image", 2, DockerRegistryService.MAX_NO_OF_TAGS_PER_PUBLIC_IMAGE);

    images = dockerPublicRegistryProcessor.paginate(dockerResponse, connectorConfig, "image", dockerRegistryRestClient);
    assertThat(images).isNotEmpty();
    assertThat(images.size()).isEqualTo(1);
    assertThat(images.get(0).getImagePath()).isEqualTo("image");
    assertThat(images.get(0).getTag()).isEqualTo("tag");
  }
}
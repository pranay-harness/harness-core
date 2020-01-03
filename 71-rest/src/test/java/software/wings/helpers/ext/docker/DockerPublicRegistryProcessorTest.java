package software.wings.helpers.ext.docker;

import static io.harness.exception.ExceptionUtils.getMessage;
import static io.harness.rule.OwnerRule.ANSHUL;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.WingsBaseTest;
import software.wings.beans.DockerConfig;
import software.wings.exception.InvalidArtifactServerException;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.io.IOException;
import java.util.List;

public class DockerPublicRegistryProcessorTest extends WingsBaseTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public WireMockRule wireMockRule = new WireMockRule(9882);

  private static final String url = "http://localhost:9882/";

  @Mock private DockerPublicImageTagResponse dockerPublicImageTagResponse;
  @Mock private DockerPublicImageTagResponse.Result result;
  @Inject @InjectMocks DockerPublicRegistryProcessor dockerPublicRegistryProcessor;

  private static DockerConfig dockerConfig =
      DockerConfig.builder().dockerRegistryUrl(url).username("username").password("password".toCharArray()).build();

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testPaginate() throws Exception {
    List<BuildDetails> images = dockerPublicRegistryProcessor.paginate(null, dockerConfig, "image", null, 10);
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
      dockerPublicRegistryProcessor.getBuilds(dockerConfig, null, "image", 10);
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
      dockerPublicRegistryProcessor.getBuilds(dockerConfig, null, "image-1", 10);
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
      dockerPublicRegistryProcessor.getBuilds(dockerConfig, null, "image-paginated", 1);
      fail("Should not reach here");
    } catch (InvalidArtifactServerException ex) {
      assertThat(getMessage(ex)).isEqualTo("Bad Request");
    }
  }
}

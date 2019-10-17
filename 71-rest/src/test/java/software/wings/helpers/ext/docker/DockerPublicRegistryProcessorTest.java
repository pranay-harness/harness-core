package software.wings.helpers.ext.docker;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.DockerConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

public class DockerPublicRegistryProcessorTest extends WingsBaseTest {
  @Mock private DockerPublicImageTagResponse dockerPublicImageTagResponse;
  @Mock private DockerPublicImageTagResponse.Result result;
  @Inject @InjectMocks DockerPublicRegistryProcessor dockerPublicRegistryProcessor;

  private DockerConfig dockerConfig;

  @Before
  public void setUp() {
    dockerConfig = DockerConfig.builder().dockerRegistryUrl("https://registry.hub.docker.com/v2/").build();
  }

  @Test
  @Category(UnitTests.class)
  public void testPaginate() throws Exception {
    List<BuildDetails> images = dockerPublicRegistryProcessor.paginate(null, dockerConfig, "image", null, 10);
    assertThat(images).isEmpty();

    when(result.getName()).thenReturn("1");
    when(dockerPublicImageTagResponse.getResults()).thenReturn(asList(result));
    when(dockerPublicImageTagResponse.getNext()).thenReturn("https://registry.hub.docker.com/v2/");
    images = dockerPublicRegistryProcessor.paginate(dockerPublicImageTagResponse, dockerConfig, "image", null, 10);
    assertThat(images).isNotEmpty();
    assertThat(images.get(0).getUiDisplayName()).isEqualTo("Tag# 1");
    assertThat(images.get(0).getBuildUrl()).isEqualTo("https://registry.hub.docker.com/v2/image/tags/1");
  }
}

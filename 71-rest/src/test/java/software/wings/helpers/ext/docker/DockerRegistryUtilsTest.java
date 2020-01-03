package software.wings.helpers.ext.docker;

import static io.harness.rule.OwnerRule.AADITI;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;

import java.util.List;
import java.util.Map;

public class DockerRegistryUtilsTest extends WingsBaseTest {
  @Mock private DockerRegistryRestClient dockerRegistryRestClient;
  @InjectMocks DockerRegistryUtils dockerRegistryUtils = new DockerRegistryUtils();
  private static final String AUTH_HEADER = "AUTH_HEADER";
  private static final String IMAGE_NAME = "IMAGE_NAME";

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldNotGetLabelsIfEmptyTags() {
    List<Map<String, String>> labelsMap =
        dockerRegistryUtils.getLabels(dockerRegistryRestClient, null, AUTH_HEADER, IMAGE_NAME, asList(), 10);
    assertThat(labelsMap).isEmpty();
  }
}

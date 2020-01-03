package software.wings.helpers.ext.docker;

import static io.harness.exception.ExceptionUtils.getMessage;
import static io.harness.rule.OwnerRule.ANSHUL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;

import com.google.inject.Inject;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.WingsBaseTest;
import software.wings.beans.DockerConfig;
import software.wings.exception.InvalidArtifactServerException;

public class DockerRegistryServiceImplTest extends WingsBaseTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public WireMockRule wireMockRule = new WireMockRule(9883);
  @Inject @InjectMocks DockerRegistryService dockerRegistryService;

  private static final String url = "http://localhost:9883/";

  private static DockerConfig dockerConfig =
      DockerConfig.builder().dockerRegistryUrl(url).username("username").password("password".toCharArray()).build();

  @Test(expected = InvalidArtifactServerException.class)
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testValidateCredentialForIOException() {
    dockerRegistryService.validateCredentials(dockerConfig, null);
  }

  @Test()
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testValidateCredentialForMissingPassword() {
    try {
      dockerConfig.setPassword(null);
      dockerRegistryService.validateCredentials(dockerConfig, null);
      fail("Should not reach here");
    } catch (Exception ex) {
      assertThat(getMessage(ex)).isEqualTo("Password is a required field along with Username");
    }
    dockerConfig.setPassword("password".toCharArray());
  }

  @Test()
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetBuildDetails() {
    try {
      dockerRegistryService.getBuilds(dockerConfig, null, "image", 10);
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
      dockerRegistryService.getBuilds(dockerConfig, null, "image_500", 10);
      fail("Should not reach here");
    } catch (Exception ex) {
      assertThat(getMessage(ex)).isEqualTo("Internal Server Error");
    }
  }
}

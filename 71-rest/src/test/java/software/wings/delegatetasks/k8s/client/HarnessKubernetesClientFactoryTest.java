package software.wings.delegatetasks.k8s.client;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

public class HarnessKubernetesClientFactoryTest extends WingsBaseTest {
  @Inject private HarnessKubernetesClientFactory harnessKubernetesClientFactory;

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testMasterURLModifier() {
    String masterURL = "https://35.197.55.65";
    String modifiedURL = harnessKubernetesClientFactory.modifyMasterUrl(masterURL);
    assertThat(modifiedURL).isEqualTo(masterURL + ":443");

    masterURL = "https://35.197.55.65:90";
    modifiedURL = harnessKubernetesClientFactory.modifyMasterUrl(masterURL);
    assertThat(modifiedURL).isEqualTo(masterURL);

    masterURL = "http://35.197.55.65";
    modifiedURL = harnessKubernetesClientFactory.modifyMasterUrl(masterURL);
    assertThat(modifiedURL).isEqualTo(masterURL + ":80");
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testMasterURLModifierException() {
    String masterURL = "invalidURL";
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> harnessKubernetesClientFactory.modifyMasterUrl(masterURL));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldNotAddOrRemoveTrailingSlash() throws Exception {
    assertThat(harnessKubernetesClientFactory.modifyMasterUrl("https://test-domain.net"))
        .isEqualTo("https://test-domain.net:443");
    assertThat(harnessKubernetesClientFactory.modifyMasterUrl("https://test-domain.net/"))
        .isEqualTo("https://test-domain.net:443/");
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldPreservePathSegments() throws Exception {
    String masterUrl = "https://int-capi-rancher.cncpl.us/k8s/clusters/c-pv9p9";
    assertThat(harnessKubernetesClientFactory.modifyMasterUrl(masterUrl))
        .isEqualTo("https://int-capi-rancher.cncpl.us:443/k8s/clusters/c-pv9p9");
  }
}

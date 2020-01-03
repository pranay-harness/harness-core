package software.wings.security.authentication;

import static io.harness.rule.OwnerRule.UJJAWAL;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

public class SimpleUrlBuilderTest extends WingsBaseTest {
  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testBuildUrl() {
    String url = new SimpleUrlBuilder("http://google.com").addQueryParam("a", "1").addQueryParam("b", "2").build();

    assertThat(url).isEqualTo("http://google.com?a=1&b=2");
  }
}

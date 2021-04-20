package software.wings.service.impl.servicenow;

import static io.harness.rule.OwnerRule.AGORODETKI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import okhttp3.OkHttpClient;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ServiceNowDelegateServiceImplTest {
  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnHttpClientWithIncreasedTimeout() {
    OkHttpClient httpClientWithIncreasedTimeout =
        ServiceNowDelegateServiceImpl.getHttpClientWithIncreasedTimeout("url.com", false);
    assertThat(httpClientWithIncreasedTimeout.connectTimeoutMillis()).isEqualTo(45000);
    assertThat(httpClientWithIncreasedTimeout.readTimeoutMillis()).isEqualTo(45000);
  }
}

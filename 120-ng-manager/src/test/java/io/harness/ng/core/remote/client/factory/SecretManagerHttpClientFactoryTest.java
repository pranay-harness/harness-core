package io.harness.ng.core.remote.client.factory;

import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.ng.core.BaseTest;
import io.harness.ng.core.SecretManagerClientConfig;
import io.harness.ng.core.remote.client.SecretManagerClient;
import io.harness.rule.Owner;
import io.harness.security.ServiceTokenGenerator;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class SecretManagerHttpClientFactoryTest extends BaseTest {
  private static final String SERVICE_SECRET = "TEST_SECRET";
  private static final String BASE_URL = "http://localhost:8080/";
  private static final long CONNECTION_TIME_OUT_IN_SECONDS = 15;
  private static final long READ_TIME_OUT_IN_SECONDS = 15;

  @Mock ServiceTokenGenerator tokenGenerator;

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGet() {
    SecretManagerClientConfig secretManagerConfig = SecretManagerClientConfig.builder()
                                                        .serviceSecret(SERVICE_SECRET)
                                                        .baseUrl(BASE_URL)
                                                        .connectTimeOutSeconds(CONNECTION_TIME_OUT_IN_SECONDS)
                                                        .readTimeOutSeconds(READ_TIME_OUT_IN_SECONDS)
                                                        .build();

    SecretManagerHttpClientFactory secretManagerHttpClientFactory =
        new SecretManagerHttpClientFactory(secretManagerConfig, tokenGenerator);
    SecretManagerClient secretManagerClient = secretManagerHttpClientFactory.get();
    assertThat(secretManagerClient).isNotNull();
  }
}

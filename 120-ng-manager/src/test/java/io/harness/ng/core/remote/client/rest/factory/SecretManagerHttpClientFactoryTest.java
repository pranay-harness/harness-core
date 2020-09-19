package io.harness.ng.core.remote.client.rest.factory;

import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.remote.SecretManagerClient;
import io.harness.secretmanagerclient.remote.SecretManagerHttpClientFactory;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class SecretManagerHttpClientFactoryTest extends CategoryTest {
  private static final String SERVICE_SECRET = "TEST_SECRET";
  private static final String BASE_URL = "http://localhost:8080/";
  private static final long CONNECTION_TIME_OUT_IN_SECONDS = 15;
  private static final long READ_TIME_OUT_IN_SECONDS = 15;

  @Mock ServiceTokenGenerator tokenGenerator;
  @Mock KryoConverterFactory kryoConverterFactory;

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGet() {
    initMocks(this);
    ServiceHttpClientConfig secretManagerConfig = ServiceHttpClientConfig.builder()
                                                      .baseUrl(BASE_URL)
                                                      .connectTimeOutSeconds(CONNECTION_TIME_OUT_IN_SECONDS)
                                                      .readTimeOutSeconds(READ_TIME_OUT_IN_SECONDS)
                                                      .build();

    SecretManagerHttpClientFactory secretManagerHttpClientFactory =
        new SecretManagerHttpClientFactory(secretManagerConfig, SERVICE_SECRET, tokenGenerator, kryoConverterFactory);
    SecretManagerClient secretManagerClient = secretManagerHttpClientFactory.get();
    assertThat(secretManagerClient).isNotNull();
  }
}

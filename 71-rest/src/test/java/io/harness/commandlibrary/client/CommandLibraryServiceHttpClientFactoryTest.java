package io.harness.commandlibrary.client;

import static io.harness.rest.RestResponse.Builder.aRestResponse;
import static io.harness.rule.OwnerRule.ROHIT_KUMAR;

import io.harness.CategoryTest;
import io.harness.category.element.FunctionalTests;
import io.harness.rule.Owner;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.JsonUtils;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.HttpURLConnection;

public class CommandLibraryServiceHttpClientFactoryTest extends CategoryTest {
  private MockWebServer mockWebServer;

  @Before
  public void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
  }

  @After
  public void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(FunctionalTests.class)
  public void test_get() throws IOException {
    final String baseUrl = mockWebServer.url("/").url().toString();
    final CommandLibraryServiceHttpClientFactory factory =
        Mockito.spy(new CommandLibraryServiceHttpClientFactory(baseUrl, new ServiceTokenGenerator()));

    mockWebServer.enqueue(new MockResponse()
                              .setResponseCode(HttpURLConnection.HTTP_OK)
                              .setBody(JsonUtils.asJson(aRestResponse().withResource("hello world").build())));

    Mockito.doReturn("secret").when(factory).getServiceSecretForManager();
    final CommandLibraryServiceHttpClient serviceHttpClient = factory.get();
    Assertions.assertThat(serviceHttpClient.getHelloWorld().execute().body().getResource()).isEqualTo("hello world");
  }
}
package io.harness.commandlibrary.client;

import static io.harness.rest.RestResponse.Builder.aRestResponse;
import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;

import com.google.common.collect.ImmutableList;

import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.commandlibrary.api.dto.CommandStoreDTO;
import io.harness.commandlibrary.common.service.CommandLibraryService;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.JsonUtils;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;

public class CommandLibraryServiceHttpClientFactoryTest extends CategoryTest {
  private MockWebServer mockWebServer;
  @Mock CommandLibraryService commandLibraryService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    mockWebServer = new MockWebServer();
    mockWebServer.start();
  }

  @After
  public void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_get() throws IOException, IllegalAccessException {
    final String baseUrl = mockWebServer.url("/").url().toString();
    final CommandLibraryServiceHttpClientFactory factory =
        new CommandLibraryServiceHttpClientFactory(baseUrl, new ServiceTokenGenerator());
    FieldUtils.writeField(factory, "commandLibraryService", commandLibraryService, true);

    mockWebServer.enqueue(new MockResponse()
                              .setResponseCode(HttpURLConnection.HTTP_OK)
                              .setBody(JsonUtils.asJson(
                                  aRestResponse()
                                      .withResource(ImmutableList.of(CommandStoreDTO.builder().name("harness").build()))
                                      .build())));

    doReturn("secret").when(commandLibraryService).getSecretForClient(anyString());
    final CommandLibraryServiceHttpClient serviceHttpClient = factory.get();

    final Object body1 = serviceHttpClient.getCommandStores(emptyMap()).execute().body();
    final RestResponse<List<CommandStoreDTO>> body =
        factory.getObjectMapper().convertValue(body1, new TypeReference<RestResponse<List<CommandStoreDTO>>>() {});

    assertThat(body.getResource().get(0).getName()).isEqualTo("harness");
  }
}
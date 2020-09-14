package io.harness.mock.server;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.harness.mock.utils.UriUtils.HTTP_200_REQUEST;
import static io.harness.mock.utils.UriUtils.HTTP_404_REQUEST;
import static io.harness.mock.utils.UriUtils.HTTP_500_REQUEST;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import org.apache.http.HttpStatus;

import java.util.ResourceBundle;

public class MockServer {
  private static final String APPLICATION = "mock/application";
  private static final String HTTP_PORT = "http-port";
  private static final String HTTPS_PORT = "https-port";

  private final WireMockServer wireMockServer;

  public MockServer() {
    ResourceBundle resourceBundle = ResourceBundle.getBundle(APPLICATION);
    int httpPort = Integer.parseInt(resourceBundle.getString(HTTP_PORT));
    int httpsPort = Integer.parseInt(resourceBundle.getString(HTTPS_PORT));
    wireMockServer =
        new WireMockServer(wireMockConfig().port(httpPort).httpsPort(httpsPort).disableRequestJournal().notifier(
            new ConsoleNotifier(true)));
  }

  public void start() {
    init();
    wireMockServer.start();
  }

  public void stop() {
    wireMockServer.stop();
  }

  public void init() {
    addStubForMockServerStatus();
    addStubForHttpStatus200();
    addStubForHttpStatus404();
    addStubForHttpStatus500();
  }

  private void addStubForMockServerStatus() {
    wireMockServer.stubFor(get(urlEqualTo("/status"))
                               .willReturn(aResponse().withHeader("content-type", "application/json").withBody("200")));
  }

  private void addStubForHttpStatus200() {
    wireMockServer.stubFor(get(urlEqualTo(HTTP_200_REQUEST)).willReturn(aResponse().withStatus(HttpStatus.SC_OK)));
  }

  private void addStubForHttpStatus404() {
    wireMockServer.stubFor(
        get(urlEqualTo(HTTP_404_REQUEST)).willReturn(aResponse().withStatus(HttpStatus.SC_NOT_FOUND)));
  }

  private void addStubForHttpStatus500() {
    wireMockServer.stubFor(
        get(urlEqualTo(HTTP_500_REQUEST)).willReturn(aResponse().withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)));
  }
}

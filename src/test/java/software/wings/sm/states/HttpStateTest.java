package software.wings.sm.states;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.api.HttpStateExecutionData.Builder.aHttpStateExecutionData;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.states.HttpState.Builder.aHttpState;

import com.google.inject.Injector;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.api.HttpStateExecutionData;
import software.wings.common.UUIDGenerator;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionInstance;

import javax.inject.Inject;

// TODO: Auto-generated Javadoc

/**
 * The Class HttpStateTest.
 *
 * @author Rishi
 */
public class HttpStateTest extends WingsBaseTest {
  private static final HttpState.Builder httpStateBuilder =
      aHttpState()
          .withName("healthCheck1")
          .withMethod("GET")
          .withUrl("http://${host.hostName}:8088/health/status")
          .withHeader("Content-Type: application/json, Accept: */*")
          .withAssertion(
              "(${httpResponseCode}==200 || ${httpResponseCode}==201) && ${xmlFormat()} && ${xpath('//health/status/text()')}.equals('Enabled')");
  /**
   * The Wire mock rule.
   */
  @Rule public WireMockRule wireMockRule = new WireMockRule(8088);
  @Inject private Injector injector;
  private ExecutionContextImpl context;

  /**
   * Sets context.
   */
  @Before
  public void setupContext() {
    StateExecutionInstance stateExecutionInstance =
        aStateExecutionInstance().withStateName("healthCheck1").withUuid(UUIDGenerator.getUuid()).build();

    context = new ExecutionContextImpl(stateExecutionInstance, null, injector);
    context.pushContextElement(aHostElement().withHostName("localhost").build());
  }

  /**
   * Should execute and evaluate response.
   */
  @Test
  public void shouldExecuteAndEvaluateResponse() {
    wireMockRule.stubFor(get(urlEqualTo("/health/status"))
                             .withHeader("Content-Type", equalTo("application/json"))
                             .withHeader("Accept", equalTo("*/*"))
                             .willReturn(aResponse()
                                             .withStatus(200)
                                             .withBody("<health><status>Enabled</status></health>")
                                             .withHeader("Content-Type", "text/xml")));

    ExecutionResponse response = httpStateBuilder.but().build().execute(context);

    assertThat(response).isNotNull().extracting(ExecutionResponse::isAsynch).containsExactly(false);
    assertThat(response.getStateExecutionData())
        .isNotNull()
        .isInstanceOf(HttpStateExecutionData.class)
        .isEqualToComparingOnlyGivenFields(aHttpStateExecutionData()
                                               .withHttpUrl("http://localhost:8088/health/status")
                                               .withAssertionStatus("SUCCESS")
                                               .withHttpResponseCode(200)
                                               .withHttpResponseBody("<health><status>Enabled</status></health>")
                                               .build(),
            "httpUrl", "assertionStatus", "httpResponseCode", "httpResponseBody");
  }

  /**
   * Should fail on socket timeout.
   */
  @Test
  public void shouldFailOnSocketTimeout() {
    wireMockRule.stubFor(get(urlEqualTo("/health/status"))
                             .withHeader("Content-Type", equalTo("application/json"))
                             .withHeader("Accept", equalTo("*/*"))
                             .willReturn(aResponse()
                                             .withStatus(200)
                                             .withBody("<health><status>Enabled</status></health>")
                                             .withHeader("Content-Type", "text/xml")
                                             .withFixedDelay(2000)));

    ExecutionResponse response = httpStateBuilder.but().withSocketTimeoutMillis(1000).build().execute(context);
    assertThat(response).isNotNull().extracting(ExecutionResponse::isAsynch).containsExactly(false);
    assertThat(response.getStateExecutionData())
        .isNotNull()
        .isInstanceOf(HttpStateExecutionData.class)
        .isEqualToComparingOnlyGivenFields(aHttpStateExecutionData()
                                               .withHttpUrl("http://localhost:8088/health/status")
                                               .withAssertionStatus("FAILED")
                                               .withHttpResponseCode(500)
                                               .withHttpResponseBody("SocketTimeoutException: Read timed out")
                                               .build(),
            "httpUrl", "assertionStatus", "httpResponseCode", "httpResponseBody");
  }

  /**
   * Should fail on empty response.
   */
  @Test
  public void shouldFailOnEmptyResponse() {
    wireMockRule.stubFor(get(urlEqualTo("/health/status"))
                             .withHeader("Content-Type", equalTo("application/json"))
                             .withHeader("Accept", equalTo("*/*"))
                             .willReturn(aResponse().withStatus(200).withFault(Fault.EMPTY_RESPONSE)));

    ExecutionResponse response = httpStateBuilder.but().build().execute(context);
    assertThat(response).isNotNull().extracting(ExecutionResponse::isAsynch).containsExactly(false);
    assertThat(response.getStateExecutionData())
        .isNotNull()
        .isInstanceOf(HttpStateExecutionData.class)
        .isEqualToComparingOnlyGivenFields(
            aHttpStateExecutionData()
                .withHttpUrl("http://localhost:8088/health/status")
                .withAssertionStatus("FAILED")
                .withHttpResponseCode(500)
                .withHttpResponseBody("NoHttpResponseException: localhost:8088 failed to respond")
                .build(),
            "httpUrl", "assertionStatus", "httpResponseCode", "httpResponseBody");
  }

  /**
   * Should fail on malformed response.
   */
  @Test
  public void shouldFailOnMalformedResponse() {
    wireMockRule.stubFor(get(urlEqualTo("/health/status"))
                             .withHeader("Content-Type", equalTo("application/json"))
                             .withHeader("Accept", equalTo("*/*"))
                             .willReturn(aResponse().withStatus(200).withFault(Fault.MALFORMED_RESPONSE_CHUNK)));

    ExecutionResponse response = httpStateBuilder.but().build().execute(context);
    assertThat(response).isNotNull().extracting(ExecutionResponse::isAsynch).containsExactly(false);
    assertThat(response.getStateExecutionData())
        .isNotNull()
        .isInstanceOf(HttpStateExecutionData.class)
        .isEqualToComparingOnlyGivenFields(aHttpStateExecutionData()
                                               .withHttpUrl("http://localhost:8088/health/status")
                                               .withAssertionStatus("FAILED")
                                               .withHttpResponseCode(500)
                                               .withHttpResponseBody("MalformedChunkCodingException: Bad chunk header")
                                               .build(),
            "httpUrl", "assertionStatus", "httpResponseCode", "httpResponseBody");
  }

  /**
   * Should fail on random data.
   */
  @Test
  public void shouldFailOnRandomData() {
    wireMockRule.stubFor(get(urlEqualTo("/health/status"))
                             .withHeader("Content-Type", equalTo("application/json"))
                             .withHeader("Accept", equalTo("*/*"))
                             .willReturn(aResponse().withStatus(200).withFault(Fault.RANDOM_DATA_THEN_CLOSE)));

    ExecutionResponse response = httpStateBuilder.but().build().execute(context);
    assertThat(response).isNotNull().extracting(ExecutionResponse::isAsynch).containsExactly(false);
    assertThat(response.getStateExecutionData())
        .isNotNull()
        .isInstanceOf(HttpStateExecutionData.class)
        .isEqualToComparingOnlyGivenFields(aHttpStateExecutionData()
                                               .withHttpUrl("http://localhost:8088/health/status")
                                               .withAssertionStatus("FAILED")
                                               .withHttpResponseCode(500)
                                               .withHttpResponseBody("ClientProtocolException: ")
                                               .build(),
            "httpUrl", "assertionStatus", "httpResponseCode", "httpResponseBody");
  }

  /**
   * Should fail on connect timeout.
   */
  @Test
  public void shouldFailOnConnectTimeout() {
    context.pushContextElement(aHostElement().withHostName("www.google.com").build());

    ExecutionResponse response =
        httpStateBuilder.but().withUrl("http://${host.hostName}:81/health/status").build().execute(context);
    assertThat(response).isNotNull().extracting(ExecutionResponse::isAsynch).containsExactly(false);
    assertThat(response.getStateExecutionData())
        .isNotNull()
        .isInstanceOf(HttpStateExecutionData.class)
        .isEqualToComparingOnlyGivenFields(aHttpStateExecutionData()
                                               .withHttpUrl("http://www.google.com:81/health/status")
                                               .withAssertionStatus("FAILED")
                                               .withHttpResponseCode(500)
                                               .build(),
            "httpUrl", "assertionStatus", "httpResponseCode");
    assertThat(((HttpStateExecutionData) response.getStateExecutionData()).getHttpResponseBody())
        .startsWith("HttpHostConnectException: Connect to www.google.com:81 ");
  }
}

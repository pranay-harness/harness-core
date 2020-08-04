package io.harness.perpetualtask.k8s.metrics.client.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static io.harness.rule.OwnerRule.UTSAV;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.io.Resources;
import com.google.gson.JsonSyntaxException;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.k8s.metrics.client.model.node.NodeMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.node.NodeMetricsList;
import io.harness.perpetualtask.k8s.metrics.client.model.pod.PodMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.pod.PodMetricsList;
import io.harness.rule.Owner;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.ClientBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@RunWith(MockitoJUnitRunner.class)
public class DefaultK8sMetricsClientTest extends CategoryTest {
  @Rule public WireMockRule wireMockRule = new WireMockRule(65218);

  JSON json;
  String resourceToString;
  DefaultK8sMetricsClient k8sMetricsClient;

  // the actual url called has params e.g., /apis/metrics.k8s.io/v1beta1/nodes?watch=false
  final String URL_REGEX_SUFFIX = "(\\?(.*))?";
  final String METRICS_API_VERSION = "metrics.k8s.io/v1beta1";

  @Before
  public void setup() {
    k8sMetricsClient =
        new DefaultK8sMetricsClient(new ClientBuilder().setBasePath("http://localhost:" + wireMockRule.port()).build());
    json = new JSON();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void checkIfK8sMetricsClientInstanceOfCoreV1Api() {
    assertThat(k8sMetricsClient).isNotNull().isInstanceOf(CoreV1Api.class);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void nodeMetricsClientTest() throws IOException {
    resourceToString = getResourceAsString("get_nodemetrics.json");

    stubFor(get(urlMatching("^/apis/" + METRICS_API_VERSION + "/nodes" + URL_REGEX_SUFFIX))
                .willReturn(aResponse().withStatus(200).withBody(resourceToString)));

    NodeMetricsList expected = json.deserialize(resourceToString, NodeMetricsList.class);
    NodeMetricsList response = k8sMetricsClient.nodeMetrics().list().getObject();

    assertThat(resourceToString).isNotNull().isNotEmpty();
    assertThat(response).isNotNull().isEqualTo(expected);
    assertThat(response.getKind()).isEqualTo(NodeMetricsList.class.getSimpleName());
    assertThat(response.getApiVersion()).isEqualTo(METRICS_API_VERSION);

    assertThat(response.getItems()).hasSizeGreaterThan(0);
    assertThat(response.getItems().get(0)).isInstanceOf(NodeMetrics.class);
    assertThat(response.getItems().get(0).getKind()).isEqualTo(NodeMetrics.class.getSimpleName());
    assertThat(response.getItems().get(0).getMetadata()).isNotNull();
    assertThat(response.getItems().get(0).getMetadata().getName()).isNotEmpty();
  }

  @Test(expected = JsonSyntaxException.class)
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void nodeMetricsClientTestThrowsJsonSyntaxException() {
    resourceToString = "404 page not found\n";

    stubFor(get(urlMatching("^/apis/" + METRICS_API_VERSION + "/nodes" + URL_REGEX_SUFFIX))
                .willReturn(aResponse().withStatus(404).withBody(resourceToString)));

    k8sMetricsClient.nodeMetrics().list().getObject();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void podMetricsClientTest() throws IOException {
    resourceToString = getResourceAsString("get_podmetrics.json");

    stubFor(get(urlMatching("^/apis/" + METRICS_API_VERSION + "/pods" + URL_REGEX_SUFFIX))
                .willReturn(aResponse().withStatus(200).withBody(resourceToString)));

    PodMetricsList expected = json.deserialize(resourceToString, PodMetricsList.class);
    PodMetricsList response = k8sMetricsClient.podMetrics().list().getObject();

    assertThat(resourceToString).isNotNull().isNotEmpty();
    assertThat(response).isNotNull().isEqualTo(expected);
    assertThat(response.getKind()).isEqualTo(PodMetricsList.class.getSimpleName());
    assertThat(response.getApiVersion()).isEqualTo(METRICS_API_VERSION);

    assertThat(response.getItems()).hasSizeGreaterThan(0);
    assertThat(response.getItems().get(0)).isInstanceOf(PodMetrics.class);
    assertThat(response.getItems().get(0).getKind()).isEqualTo(PodMetrics.class.getSimpleName());
    assertThat(response.getItems().get(0).getMetadata()).isNotNull();
    assertThat(response.getItems().get(0).getMetadata().getName()).isNotEmpty();
  }

  @Test(expected = JsonSyntaxException.class)
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void podMetricsClientTestThrowsJsonSyntaxException() {
    resourceToString = "404 page not found\n";

    stubFor(get(urlMatching("^/apis/" + METRICS_API_VERSION + "/pods" + URL_REGEX_SUFFIX))
                .willReturn(aResponse().withStatus(404).withBody(resourceToString)));

    k8sMetricsClient.podMetrics().list().getObject();
  }

  private String getResourceAsString(String filename) throws IOException {
    URL url = getClass().getClassLoader().getResource("k8sapidump/" + filename);
    return Resources.toString(url, StandardCharsets.UTF_8);
  }
}
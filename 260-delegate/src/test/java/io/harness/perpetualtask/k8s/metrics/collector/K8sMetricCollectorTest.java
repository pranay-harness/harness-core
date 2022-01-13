/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask.k8s.metrics.collector;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.rule.OwnerRule.AVMOHAN;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ccm.commons.constants.Constants;
import io.harness.event.client.EventPublisher;
import io.harness.event.payloads.AggregatedStorage;
import io.harness.event.payloads.AggregatedUsage;
import io.harness.event.payloads.NodeMetric;
import io.harness.event.payloads.PVMetric;
import io.harness.event.payloads.PodMetric;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.harness.perpetualtask.k8s.metrics.client.impl.DefaultK8sMetricsClient;
import io.harness.perpetualtask.k8s.metrics.client.model.Usage;
import io.harness.perpetualtask.k8s.metrics.client.model.node.NodeMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.node.NodeMetricsList;
import io.harness.perpetualtask.k8s.metrics.client.model.pod.PodMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.pod.PodMetricsList;
import io.harness.rule.Owner;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Durations;
import io.kubernetes.client.util.ClientBuilder;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@OwnedBy(CE)
public class K8sMetricCollectorTest extends CategoryTest {
  private static final ClusterDetails CLUSTER_DETAILS = ClusterDetails.builder()
                                                            .clusterName("test-cluster-name")
                                                            .clusterId("5c14dfd1-24a2-4ccf-a95c-dbe1d2879345")
                                                            .cloudProviderId("4412d653-3c5d-46bc-ba37-d926f99c7a4a")
                                                            .kubeSystemUid("aa4062a7-d214-4642-8bb5-dfc32e750ed0")
                                                            .build();
  @Rule public WireMockRule wireMockRule = new WireMockRule(65219);

  private K8sMetricCollector k8sMetricCollector;
  private final String URL_REGEX_SUFFIX = "(\\?(.*))?";

  @Mock private EventPublisher eventPublisher;
  @Captor private ArgumentCaptor<Message> messageArgumentCaptor;
  private DefaultK8sMetricsClient k8sMetricsClient;

  @Before
  public void setUp() throws Exception {
    eventPublisher = mock(EventPublisher.class);
    k8sMetricsClient =
        new DefaultK8sMetricsClient(new ClientBuilder().setBasePath("http://localhost:" + wireMockRule.port()).build());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldNotPublishMetricsIfNotAggregationWindowPassed() {
    Instant now = Instant.now();

    stubFor(
        get(urlPathEqualTo("/apis/metrics.k8s.io/v1beta1/nodes"))
            .willReturn(aResponse().withStatus(200).withBody(new Gson().toJson(
                NodeMetricsList.builder()
                    .items(ImmutableList.of(NodeMetrics.builder()
                                                .name("node1-name")
                                                .timestamp("2019-11-26T07:00:32Z")
                                                .window("30s")
                                                .usage(Usage.builder().cpu("746640510n").memory("6825124Ki").build())
                                                .build(),
                        NodeMetrics.builder()
                            .name("node2-name")
                            .timestamp("2019-11-26T07:00:28Z")
                            .window("30s")
                            .usage(Usage.builder().cpu("2938773795n").memory("18281752Ki").build())
                            .build()))
                    .build()))));

    stubFor(get(urlPathEqualTo("/apis/metrics.k8s.io/v1beta1/pods"))
                .willReturn(aResponse().withStatus(200).withBody(new Gson().toJson(
                    PodMetricsList.builder()
                        .item(PodMetrics.builder()
                                  .name("pod1")
                                  .namespace("ns1")
                                  .timestamp("2019-11-26T07:00:32Z")
                                  .window("30s")
                                  .container(PodMetrics.Container.builder()
                                                 .name("p1-ctr1")
                                                 .usage(Usage.builder().cpu("41181421n").memory("139304Ki").build())
                                                 .build())
                                  .build())
                        .item(PodMetrics.builder()
                                  .name("pod2")
                                  .namespace("ns1")
                                  .timestamp("2019-11-26T07:00:32Z")
                                  .window("30s")
                                  .container(PodMetrics.Container.builder()
                                                 .name("p2-ctr1")
                                                 .usage(Usage.builder().cpu("185503n").memory("7460Ki").build())
                                                 .build())
                                  .container(PodMetrics.Container.builder()
                                                 .name("p2-ctr2")
                                                 .usage(Usage.builder().cpu("735522992n").memory("225144Ki").build())
                                                 .build())
                                  .build())

                        .build()))));

    stubFor(get(urlMatching("^/api/v1/nodes/node1-name/proxy/stats/summary" + URL_REGEX_SUFFIX))
                .willReturn(aResponse().withStatus(200).withBody("{}")));
    stubFor(get(urlMatching("^/api/v1/nodes/node2-name/proxy/stats/summary" + URL_REGEX_SUFFIX))
                .willReturn(aResponse().withStatus(200).withBody("some random response")));

    k8sMetricCollector = new K8sMetricCollector(eventPublisher, CLUSTER_DETAILS, now.minus(10, ChronoUnit.MINUTES));
    doNothing()
        .when(eventPublisher)
        .publishMessage(messageArgumentCaptor.capture(), any(Timestamp.class),
            eq(Collections.singletonMap(Constants.CLUSTER_ID_IDENTIFIER, CLUSTER_DETAILS.getClusterId())));
    k8sMetricCollector.collectAndPublishMetrics(k8sMetricsClient, now, k8sMetricsClient, null);
    verifyZeroInteractions(eventPublisher);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldPublishMetrics() throws IOException {
    Instant now = Instant.now();

    stubFor(
        get(urlPathEqualTo("/apis/metrics.k8s.io/v1beta1/nodes"))
            .inScenario("nodes")
            .willSetStateTo("2nd")
            .willReturn(aResponse().withStatus(200).withBody(new Gson().toJson(
                NodeMetricsList.builder()
                    .items(ImmutableList.of(NodeMetrics.builder()
                                                .name("node1-name")
                                                .timestamp("2019-11-26T07:00:32Z")
                                                .window("30s")
                                                .usage(Usage.builder().cpu("746640510n").memory("6825124Ki").build())
                                                .build(),
                        NodeMetrics.builder()
                            .name("node2-name")
                            .timestamp("2019-11-26T07:00:28Z")
                            .window("30s")
                            .usage(Usage.builder().cpu("2938773795n").memory("18281752Ki").build())
                            .build()))
                    .build()))));

    stubFor(
        get(urlPathEqualTo("/apis/metrics.k8s.io/v1beta1/nodes"))
            .inScenario("nodes")
            .whenScenarioStateIs("2nd")
            .willReturn(aResponse().withStatus(200).withBody(new Gson().toJson(
                NodeMetricsList.builder()
                    .items(ImmutableList.of(NodeMetrics.builder()
                                                .name("node1-name")
                                                .timestamp("2019-11-26T07:01:02Z")
                                                .window("30s")
                                                .usage(Usage.builder().cpu("826640233n").memory("7443423Ki").build())
                                                .build(),
                        NodeMetrics.builder()
                            .name("node2-name")
                            .timestamp("2019-11-26T07:00:58Z")
                            .window("30s")
                            .usage(Usage.builder().cpu("3425434234n").memory("25316652Ki").build())
                            .build()))
                    .build()))));

    stubFor(get(urlPathEqualTo("/apis/metrics.k8s.io/v1beta1/pods"))
                .inScenario("pods")
                .willSetStateTo("2nd")
                .willReturn(aResponse().withStatus(200).withBody(new Gson().toJson(
                    PodMetricsList.builder()
                        .item(PodMetrics.builder()
                                  .name("pod1")
                                  .namespace("ns1")
                                  .timestamp("2019-11-26T07:00:32Z")
                                  .window("30s")
                                  .container(PodMetrics.Container.builder()
                                                 .name("p1-ctr1")
                                                 .usage(Usage.builder().cpu("41181421n").memory("139304Ki").build())
                                                 .build())
                                  .build())
                        .item(PodMetrics.builder()
                                  .name("pod2")
                                  .namespace("ns1")
                                  .timestamp("2019-11-26T07:00:32Z")
                                  .window("30s")
                                  .container(PodMetrics.Container.builder()
                                                 .name("p2-ctr1")
                                                 .usage(Usage.builder().cpu("185503n").memory("7460Ki").build())
                                                 .build())
                                  .container(PodMetrics.Container.builder()
                                                 .name("p2-ctr2")
                                                 .usage(Usage.builder().cpu("735522992n").memory("225144Ki").build())
                                                 .build())
                                  .build())
                        .build()))));
    stubFor(get(urlPathEqualTo("/apis/metrics.k8s.io/v1beta1/pods"))
                .inScenario("pods")
                .whenScenarioStateIs("2nd")
                .willReturn(aResponse().withStatus(200).withBody(new Gson().toJson(
                    PodMetricsList.builder()
                        .item(PodMetrics.builder()
                                  .name("pod1")
                                  .namespace("ns1")
                                  .timestamp("2019-11-26T07:01:02Z")
                                  .window("30s")
                                  .container(PodMetrics.Container.builder()
                                                 .name("p1-ctr1")
                                                 .usage(Usage.builder().cpu("45299563n").memory("122865Ki").build())
                                                 .build())
                                  .build())
                        .item(PodMetrics.builder()
                                  .name("pod2")
                                  .namespace("ns1")
                                  .timestamp("2019-11-26T07:01:02Z")
                                  .window("30s")
                                  .container(PodMetrics.Container.builder()
                                                 .name("p2-ctr1")
                                                 .usage(Usage.builder().cpu("209618n").memory("9101Ki").build())
                                                 .build())
                                  .container(PodMetrics.Container.builder()
                                                 .name("p2-ctr2")
                                                 .usage(Usage.builder().cpu("647260232n").memory("240904Ki").build())
                                                 .build())
                                  .build())
                        .build())))); // always

    String resourceToString = getK8sApiResponseAsString("get_nodestatssummary.json");
    stubFor(get(urlMatching("^/api/v1/nodes/node[12]-name/proxy/stats/summary" + URL_REGEX_SUFFIX))
                .willReturn(aResponse().withStatus(200).withBody(resourceToString)));

    k8sMetricCollector = new K8sMetricCollector(eventPublisher, CLUSTER_DETAILS, now);
    doNothing()
        .when(eventPublisher)
        .publishMessage(messageArgumentCaptor.capture(), any(Timestamp.class),
            eq(Collections.singletonMap(Constants.CLUSTER_ID_IDENTIFIER, CLUSTER_DETAILS.getClusterId())));
    k8sMetricCollector.collectAndPublishMetrics(
        k8sMetricsClient, now.plus(30, ChronoUnit.SECONDS), k8sMetricsClient, null);
    k8sMetricCollector.collectAndPublishMetrics(
        k8sMetricsClient, now.plus(30, ChronoUnit.MINUTES), k8sMetricsClient, null);

    verify(2, getRequestedFor(urlMatching("^/api/v1/nodes/node[12]-name/proxy/stats/summary" + URL_REGEX_SUFFIX)));

    assertThat(messageArgumentCaptor.getAllValues())
        .contains(NodeMetric.newBuilder()
                      .setCloudProviderId(CLUSTER_DETAILS.getCloudProviderId())
                      .setClusterId(CLUSTER_DETAILS.getClusterId())
                      .setKubeSystemUid(CLUSTER_DETAILS.getKubeSystemUid())
                      .setName("node1-name")
                      .setTimestamp(HTimestamps.parse("2019-11-26T07:00:32Z"))
                      .setWindow(Durations.fromSeconds(60))
                      .setAggregatedUsage(AggregatedUsage.newBuilder()
                                              .setAvgCpuNano(786640371L)
                                              .setAvgMemoryByte(7305496064L)
                                              .setMaxCpuNano(826640233L)
                                              .setMaxMemoryByte(7622065152L)
                                              .build())
                      .build(),
            NodeMetric.newBuilder()
                .setCloudProviderId(CLUSTER_DETAILS.getCloudProviderId())
                .setClusterId(CLUSTER_DETAILS.getClusterId())
                .setKubeSystemUid(CLUSTER_DETAILS.getKubeSystemUid())
                .setName("node2-name")
                .setTimestamp(HTimestamps.parse("2019-11-26T07:00:28Z"))
                .setWindow(Durations.fromSeconds(60))
                .setAggregatedUsage(AggregatedUsage.newBuilder()
                                        .setAvgCpuNano(3182104014L)
                                        .setAvgMemoryByte(22322382848L)
                                        .setMaxCpuNano(3425434234L)
                                        .setMaxMemoryByte(25924251648L)
                                        .build())
                .build(),
            PodMetric.newBuilder()
                .setCloudProviderId(CLUSTER_DETAILS.getCloudProviderId())
                .setClusterId(CLUSTER_DETAILS.getClusterId())
                .setKubeSystemUid(CLUSTER_DETAILS.getKubeSystemUid())
                .setName("pod1")
                .setNamespace("ns1")
                .setTimestamp(HTimestamps.parse("2019-11-26T07:00:32Z"))
                .setWindow(Durations.fromSeconds(60))
                .setAggregatedUsage(AggregatedUsage.newBuilder()
                                        .setAvgCpuNano(43240492L)
                                        .setAvgMemoryByte(134230528L)
                                        .setMaxCpuNano(45299563L)
                                        .setMaxMemoryByte(142647296L)
                                        .build())
                .build(),
            PodMetric.newBuilder()
                .setCloudProviderId(CLUSTER_DETAILS.getCloudProviderId())
                .setClusterId(CLUSTER_DETAILS.getClusterId())
                .setKubeSystemUid(CLUSTER_DETAILS.getKubeSystemUid())
                .setName("pod2")
                .setNamespace("ns1")
                .setTimestamp(HTimestamps.parse("2019-11-26T07:00:32Z"))
                .setWindow(Durations.fromSeconds(60))
                .setAggregatedUsage(AggregatedUsage.newBuilder()
                                        .setAvgCpuNano(691589172)
                                        .setAvgMemoryByte(247095808L)
                                        .setMaxCpuNano(735708495)
                                        .setMaxMemoryByte(256005120L)
                                        .build())
                .build(),
            PVMetric.newBuilder()
                .setCloudProviderId(CLUSTER_DETAILS.getCloudProviderId())
                .setClusterId(CLUSTER_DETAILS.getClusterId())
                .setKubeSystemUid(CLUSTER_DETAILS.getKubeSystemUid())
                .setName("delegate-scope/datadir-mongo-replicaset-2")
                .setPodUid("327d830d-e485-4964-9d97-992f97ee4f6f")
                .setNamespace("delegate-scope")
                .setTimestamp(HTimestamps.parse("2020-09-01T20:07:13Z"))
                .setWindow(Durations.fromSeconds(0))
                .setAggregatedStorage(
                    AggregatedStorage.newBuilder().setAvgCapacityByte(78729973760L).setAvgUsedByte(1101856768L).build())
                .build());
  }

  private String getK8sApiResponseAsString(String filename) throws IOException {
    URL url = getClass().getClassLoader().getResource("k8sapidump/" + filename);
    return Resources.toString(url, StandardCharsets.UTF_8);
  }
}

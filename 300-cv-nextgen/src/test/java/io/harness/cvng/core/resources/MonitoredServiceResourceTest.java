/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.resources;

import static io.harness.rule.OwnerRule.ABHIJITH;
import static io.harness.rule.OwnerRule.DEEPAK_CHHIKARA;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.core.beans.HealthSourceMetricDefinition;
import io.harness.cvng.core.beans.PrometheusMetricDefinition;
import io.harness.cvng.core.beans.monitoredService.HealthSource;
import io.harness.cvng.core.beans.monitoredService.MetricDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceResponse;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.PrometheusHealthSourceSpec;
import io.harness.persistence.HPersistence;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.rule.ResourceTestRule;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.yaml.snakeyaml.Yaml;

public class MonitoredServiceResourceTest extends CvNextGenTestBase {
  @Inject private Injector injector;
  @Inject private HPersistence hPersistence;

  private BuilderFactory builderFactory;
  private static MonitoredServiceResource monitoredServiceResource = new MonitoredServiceResource();

  @ClassRule
  public static final ResourceTestRule RESOURCES =
      ResourceTestRule.builder().addResource(monitoredServiceResource).build();
  @Before
  public void setup() {
    injector.injectMembers(monitoredServiceResource);
    builderFactory = BuilderFactory.getDefault();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetSloMetrics() throws IOException {
    String monitoredServiceYaml = getResource("monitoredservice/monitored-service-prometheus.yaml");

    Response createResponse = RESOURCES.client()
                                  .target("http://localhost:9998/monitored-service/")
                                  .queryParam("accountId", builderFactory.getContext().getAccountId())
                                  .request(MediaType.APPLICATION_JSON_TYPE)
                                  .post(Entity.json(convertToJson(monitoredServiceYaml)));
    assertThat(createResponse.getStatus()).isEqualTo(200);

    Response response =
        RESOURCES.client()
            .target("http://localhost:9998/monitored-service/MSIdentifier/health-source/test/slo-metrics")
            .queryParam("accountId", builderFactory.getContext().getAccountId())
            .queryParam("projectIdentifier", "cvng_proj_fve79nRfOe")
            .queryParam("orgIdentifier", "cvng_org_gc5qeLWq1W")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .get();
    assertThat(response.getStatus()).isEqualTo(200);
    RestResponse<List<MetricDTO>> restResponse =
        response.readEntity(new GenericType<RestResponse<List<MetricDTO>>>() {});
    List<MetricDTO> metricDTOS = restResponse.getResource();
    assertThat(metricDTOS).hasSize(1);
    assertThat(metricDTOS.get(0).getMetricName()).isEqualTo("Prometheus Metric");
    assertThat(metricDTOS.get(0).getIdentifier()).isEqualTo("PrometheusMetric");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testSaveMonitoredService_withMetricDefIdentifier() throws IOException {
    String monitoredServiceYaml = getResource("monitoredservice/monitored-service-prometheus.yaml");

    Response response = RESOURCES.client()
                            .target("http://localhost:9998/monitored-service/")
                            .queryParam("accountId", builderFactory.getContext().getAccountId())
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .post(Entity.json(convertToJson(monitoredServiceYaml)));
    assertThat(response.getStatus()).isEqualTo(200);
    RestResponse<MonitoredServiceResponse> restResponse =
        response.readEntity(new GenericType<RestResponse<MonitoredServiceResponse>>() {});
    MonitoredServiceDTO monitoredServiceDTO = restResponse.getResource().getMonitoredServiceDTO();
    assertThat(monitoredServiceDTO.getSources().getHealthSources()).hasSize(1);
    HealthSource healthSource = monitoredServiceDTO.getSources().getHealthSources().iterator().next();
    HealthSourceMetricDefinition healthSourceMetricDefinition =
        ((PrometheusHealthSourceSpec) healthSource.getSpec()).getMetricDefinitions().get(0);
    // assertThat(healthSourceMetricDefinition.getIdentifier()).isEqualTo("prometheus_metric123");
    assertThat(healthSourceMetricDefinition.getIdentifier())
        .isEqualTo("PrometheusMetric"); // TODO: remove this after enabling validation.
  }

  @Test
  @Owner(developers = DEEPAK_CHHIKARA)
  @Category(UnitTests.class)
  public void testSaveMonitoredService_withOnlySLIEnabled() throws IOException {
    String monitoredServiceYaml = getResource("monitoredservice/monitored-service-prometheus-only-sli.yaml");

    Response response = RESOURCES.client()
                            .target("http://localhost:9998/monitored-service/")
                            .queryParam("accountId", builderFactory.getContext().getAccountId())
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .post(Entity.json(convertToJson(monitoredServiceYaml)));
    assertThat(response.getStatus()).isEqualTo(200);
    RestResponse<MonitoredServiceResponse> restResponse =
        response.readEntity(new GenericType<RestResponse<MonitoredServiceResponse>>() {});
    MonitoredServiceDTO monitoredServiceDTO = restResponse.getResource().getMonitoredServiceDTO();
    assertThat(monitoredServiceDTO.getSources().getHealthSources()).hasSize(1);
    PrometheusMetricDefinition metricDefinition = ((PrometheusHealthSourceSpec) monitoredServiceDTO.getSources()
                                                       .getHealthSources()
                                                       .stream()
                                                       .findAny()
                                                       .get()
                                                       .getSpec())
                                                      .getMetricDefinitions()
                                                      .get(0);
    assertThat(metricDefinition.getSli().getEnabled()).isTrue();
    assertThat(metricDefinition.getAnalysis().getRiskProfile().getThresholdTypes()).isEmpty();
    // TODO Need to be remove the default behaviour
    assertThat(metricDefinition.getAnalysis().getRiskProfile().getCategory()).isEqualTo(CVMonitoringCategory.ERRORS);
    assertThat(metricDefinition.getAnalysis().getLiveMonitoring().getEnabled()).isFalse();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testSaveMonitoredService_invalidYAMLForMetricDefWithSameIdentifiers() throws IOException {
    String monitoredServiceYaml =
        getResource("monitoredservice/monitored-service-invalid-metric-def-duplicate-identifier.yaml");

    Response response = RESOURCES.client()
                            .target("http://localhost:9998/monitored-service/")
                            .queryParam("accountId", builderFactory.getContext().getAccountId())
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .post(Entity.json(convertToJson(monitoredServiceYaml)));
    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.readEntity(String.class))
        .contains("{\"field\":\"metricDefinitions\",\"message\":\"same identifier is used by multiple entities\"}");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testSaveMonitoredService_missingIdentifierHealthSoruce() throws IOException {
    String monitoredServiceYaml =
        getResource("monitoredservice/monitored-service-prometheus-invalid-empty-identifier.yaml");

    Response response = RESOURCES.client()
                            .target("http://localhost:9998/monitored-service/")
                            .queryParam("accountId", builderFactory.getContext().getAccountId())
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .post(Entity.json(convertToJson(monitoredServiceYaml)));
    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.readEntity(String.class)).contains("\"field\":\"identifier\",\"message\":\"cannot be empty\"");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  @Ignore("Enable after ui start sending identifier")
  public void testSaveMonitoredService_invalidMetricDefIdentifier() throws IOException {
    String monitoredServiceYaml = getResource("monitoredservice/monitored-service-invalid-metric-def-identifier.yaml");

    Response response = RESOURCES.client()
                            .target("http://localhost:9998/monitored-service/")
                            .queryParam("accountId", builderFactory.getContext().getAccountId())
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .post(Entity.json(convertToJson(monitoredServiceYaml)));
    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.readEntity(String.class))
        .contains(
            "{\"field\":\"identifier\",\"message\":\"can be 64 characters long and can only contain alphanumeric, underscore and $ characters, and not start with a number\"}");
  }

  private static String convertToJson(String yamlString) {
    Yaml yaml = new Yaml();
    Map<String, Object> map = yaml.load(yamlString);

    JSONObject jsonObject = new JSONObject(map);
    return jsonObject.toString();
  }
}

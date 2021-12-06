package io.harness.cvng.servicelevelobjective.services.impl;

import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.core.beans.monitoredService.HealthSource;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.beans.params.PageParams;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.cvng.servicelevelobjective.beans.SLODashboardApiFilter;
import io.harness.cvng.servicelevelobjective.beans.SLODashboardWidget;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelObjectiveDTO;
import io.harness.cvng.servicelevelobjective.services.api.SLODashboardService;
import io.harness.cvng.servicelevelobjective.services.api.ServiceLevelObjectiveService;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SLODashboardServiceImplTest extends CvNextGenTestBase {
  @Inject private SLODashboardService sloDashboardService;
  @Inject private ServiceLevelObjectiveService serviceLevelObjectiveService;
  @Inject private MonitoredServiceService monitoredServiceService;
  @Inject private MetricPackService metricPackService;
  private BuilderFactory builderFactory;
  @Before
  public void setup() {
    builderFactory = BuilderFactory.getDefault();
    metricPackService.createDefaultMetricPackAndThresholds(builderFactory.getContext().getAccountId(),
        builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetSloDashboardWidgets_emptyResponse() {
    PageResponse<SLODashboardWidget> pageResponse =
        sloDashboardService.getSloDashboardWidgets(builderFactory.getProjectParams(),
            SLODashboardApiFilter.builder().build(), PageParams.builder().page(0).size(4).build());
    assertThat(pageResponse.getPageItemCount()).isEqualTo(0);
    assertThat(pageResponse.getTotalItems()).isEqualTo(0);
    assertThat(pageResponse.getContent()).isEmpty();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetSloDashboardWidgets_withSLOs() {
    String monitoredServiceIdentifier = "monitoredServiceIdentifier";
    MonitoredServiceDTO monitoredServiceDTO =
        builderFactory.monitoredServiceDTOBuilder().identifier(monitoredServiceIdentifier).build();
    HealthSource healthSource = monitoredServiceDTO.getSources().getHealthSources().iterator().next();
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);
    ServiceLevelObjectiveDTO serviceLevelObjective = builderFactory.getServiceLevelObjectiveDTOBuilder()
                                                         .monitoredServiceRef(monitoredServiceIdentifier)
                                                         .healthSourceRef(healthSource.getIdentifier())
                                                         .build();

    serviceLevelObjectiveService.create(builderFactory.getProjectParams(), serviceLevelObjective);
    PageResponse<SLODashboardWidget> pageResponse =
        sloDashboardService.getSloDashboardWidgets(builderFactory.getProjectParams(),
            SLODashboardApiFilter.builder().build(), PageParams.builder().page(0).size(4).build());
    assertThat(pageResponse.getPageItemCount()).isEqualTo(1);
    assertThat(pageResponse.getTotalItems()).isEqualTo(1);
    List<SLODashboardWidget> sloDashboardWidgets = pageResponse.getContent();
    assertThat(sloDashboardWidgets).hasSize(1);
    assertThat(sloDashboardWidgets.get(0).getHealthSourceIdentifier()).isEqualTo(healthSource.getIdentifier());
    assertThat(sloDashboardWidgets.get(0).getHealthSourceName()).isEqualTo(healthSource.getName());
    assertThat(sloDashboardWidgets.get(0).getMonitoredServiceIdentifier()).isEqualTo(monitoredServiceIdentifier);
    assertThat(sloDashboardWidgets.get(0).getMonitoredServiceName()).isEqualTo(monitoredServiceDTO.getName());
    assertThat(sloDashboardWidgets.get(0).getTags()).isEqualTo(getNGTags(serviceLevelObjective.getTags()));
    assertThat(sloDashboardWidgets.get(0).getType())
        .isEqualTo(serviceLevelObjective.getServiceLevelIndicators().get(0).getType());
  }

  private List<NGTag> getNGTags(Map<String, String> tags) {
    return tags.entrySet()
        .stream()
        .map(entry -> NGTag.builder().key(entry.getKey()).value(entry.getValue()).build())
        .collect(Collectors.toList());
  }
}
/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans.monitoredService;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KANHAIYA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class MetricPackDTOTest extends CvNextGenTestBase {
  MetricPackDTO metricPackDTO;
  @Inject MetricPackService metricPackService;
  String orgIdentifier;
  String projectIdentifier;
  String accountId;

  @Before
  public void setup() {
    orgIdentifier = "org";
    projectIdentifier = "project";
    accountId = generateUuid();
    metricPackDTO = metricPackDTO.builder().identifier(CVMonitoringCategory.ERRORS).build();
    metricPackService.createDefaultMetricPackAndThresholds(accountId, orgIdentifier, projectIdentifier);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testToMetricPack() {
    MetricPack metricPack = metricPackDTO.toMetricPack(
        accountId, orgIdentifier, projectIdentifier, DataSourceType.APP_DYNAMICS, metricPackService);
    assertThat(metricPack.getAccountId()).isEqualTo(accountId);
    assertThat(metricPack.getOrgIdentifier()).isEqualTo(orgIdentifier);
    assertThat(metricPack.getProjectIdentifier()).isEqualTo(projectIdentifier);
    assertThat(metricPack.getIdentifier()).isEqualTo(CVMonitoringCategory.ERRORS.getDisplayName());
    assertThat(metricPack.getCategory()).isEqualTo(CVMonitoringCategory.ERRORS);
    assertThat(metricPack.getMetrics().size()).isEqualTo(1);
    assertThat(metricPack.getDataSourceType()).isEqualTo(DataSourceType.APP_DYNAMICS);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testToMetricPackDTO() {
    MetricPack metricPack = createMetricPack();
    MetricPackDTO metricPackDTO = MetricPackDTO.toMetricPackDTO(metricPack);
    assertThat(metricPackDTO.getIdentifier()).isEqualTo(metricPack.getCategory());
  }

  private MetricPack createMetricPack() {
    return MetricPack.builder()
        .accountId(accountId)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .identifier(CVMonitoringCategory.ERRORS.getDisplayName())
        .category(CVMonitoringCategory.ERRORS)
        .dataSourceType(DataSourceType.APP_DYNAMICS)
        .build();
  }
}

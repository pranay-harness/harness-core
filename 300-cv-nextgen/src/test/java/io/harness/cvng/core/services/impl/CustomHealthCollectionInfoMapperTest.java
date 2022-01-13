/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.impl;

import static io.harness.rule.OwnerRule.ANJAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.CustomHealthDataCollectionInfo;
import io.harness.cvng.beans.MetricResponseMappingDTO;
import io.harness.cvng.core.beans.HealthSourceMetricDefinition;
import io.harness.cvng.core.beans.HealthSourceQueryType;
import io.harness.cvng.core.beans.RiskProfile;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.MetricResponseMapping;
import io.harness.cvng.core.entities.CustomHealthCVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.VerificationTask.TaskType;
import io.harness.delegate.beans.connector.customhealthconnector.CustomHealthMethod;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CustomHealthCollectionInfoMapperTest extends CvNextGenTestBase {
  @Inject private CustomHealthDataCollectionInfoMapper customHealthMapper;
  CustomHealthCVConfig customHealthCVConfig;

  String groupName = "group1";
  String metricName = "metric_1";
  String urlPath = "https://dd.com";
  String metricValueJSONPath = "metricValuePath";
  String timestampJSONPath = "timeStringPath";

  @Before
  public void setup() {
    List<CustomHealthCVConfig.MetricDefinition> metricDefinitions = new ArrayList<>();
    MetricResponseMapping responseMapping = MetricResponseMapping.builder()
                                                .metricValueJsonPath(metricValueJSONPath)
                                                .timestampJsonPath(timestampJSONPath)
                                                .build();

    CustomHealthCVConfig.MetricDefinition metricDefinition =
        CustomHealthCVConfig.MetricDefinition.builder()
            .method(CustomHealthMethod.GET)
            .queryType(HealthSourceQueryType.HOST_BASED)
            .metricResponseMapping(responseMapping)
            .metricName(metricName)
            .analysis(HealthSourceMetricDefinition.AnalysisDTO.builder().build())
            .riskProfile(RiskProfile.builder().build())
            .sli(HealthSourceMetricDefinition.SLIDTO.builder().build())
            .urlPath(urlPath)
            .build();

    metricDefinitions.add(metricDefinition);
    customHealthCVConfig = CustomHealthCVConfig.builder()
                               .groupName(groupName)
                               .metricDefinitions(metricDefinitions)
                               .metricPack(MetricPack.builder().build())
                               .build();
  }

  @Test
  @Owner(developers = ANJAN)
  @Category(UnitTests.class)
  public void testToDataCollectionInfo() {
    CustomHealthDataCollectionInfo customHealthDataCollectionInfo =
        customHealthMapper.toDataCollectionInfo(customHealthCVConfig, TaskType.DEPLOYMENT);
    assertThat(customHealthDataCollectionInfo.getGroupName()).isEqualTo(groupName);
    List<CustomHealthDataCollectionInfo.CustomHealthMetricInfo> customHealthMetricInfo =
        customHealthDataCollectionInfo.getMetricInfoList();
    assertThat(customHealthMetricInfo.size()).isEqualTo(1);

    CustomHealthDataCollectionInfo.CustomHealthMetricInfo customHealthMetricInfo1 = customHealthMetricInfo.get(0);
    assertThat(customHealthMetricInfo1.getMetricName()).isEqualTo(metricName);
    assertThat(customHealthMetricInfo1.getUrlPath()).isEqualTo(urlPath);
    assertThat(customHealthMetricInfo1.getResponseMapping())
        .isEqualTo(MetricResponseMappingDTO.builder()
                       .metricValueJsonPath(metricValueJSONPath)
                       .timestampJsonPath(timestampJSONPath)
                       .build());
  }
}

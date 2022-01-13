/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.entities;

import static io.harness.rule.OwnerRule.ANJAN;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.core.beans.HealthSourceMetricDefinition;
import io.harness.cvng.core.beans.HealthSourceQueryType;
import io.harness.cvng.core.beans.RiskProfile;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.MetricResponseMapping;
import io.harness.delegate.beans.connector.customhealthconnector.CustomHealthMethod;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CustomHealthCVConfigTest extends CategoryTest {
  List<CustomHealthCVConfig.MetricDefinition> metricDefinitions;
  CustomHealthCVConfig customHealthCVConfig;
  MetricResponseMapping responseMapping;

  @Before
  public void setup() {
    metricDefinitions = new ArrayList<>();
    responseMapping = MetricResponseMapping.builder()
                          .metricValueJsonPath("metricValuePath")
                          .timestampJsonPath("timeStringPath")
                          .build();

    CustomHealthCVConfig.MetricDefinition metricDefinition =
        CustomHealthCVConfig.MetricDefinition.builder()
            .method(CustomHealthMethod.GET)
            .queryType(HealthSourceQueryType.HOST_BASED)
            .metricResponseMapping(responseMapping)
            .metricName("metric_1")
            .analysis(HealthSourceMetricDefinition.AnalysisDTO.builder().build())
            .riskProfile(RiskProfile.builder().build())
            .sli(HealthSourceMetricDefinition.SLIDTO.builder().build())
            .urlPath("https://dd.com")
            .build();

    metricDefinitions.add(metricDefinition);
    customHealthCVConfig =
        CustomHealthCVConfig.builder().groupName("group1").metricDefinitions(metricDefinitions).build();
  }

  @Test
  @Owner(developers = ANJAN)
  @Category(UnitTests.class)
  public void testValidateParams_whenMetricNameIsNull() {
    metricDefinitions.get(0).setMetricName(null);
    assertThatThrownBy(customHealthCVConfig::validateParams)
        .isInstanceOf(NullPointerException.class)
        .hasMessage("metricName should not be null for index 0");
  }

  @Test
  @Owner(developers = ANJAN)
  @Category(UnitTests.class)
  public void testValidateParams_whenLiveMonitoringIsTrueForHostBasedQuery() {
    metricDefinitions.get(0).getAnalysis().setLiveMonitoring(
        HealthSourceMetricDefinition.AnalysisDTO.LiveMonitoringDTO.builder().enabled(true).build());
    assertThatThrownBy(customHealthCVConfig::validateParams)
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Host based queries can only be used for deployment verification.");
  }

  @Test
  @Owner(developers = ANJAN)
  @Category(UnitTests.class)
  public void testValidateParams_whenDeploymentVerificationIsTrueForServiceBasedQuery() {
    metricDefinitions.get(0).getAnalysis().setDeploymentVerification(
        HealthSourceMetricDefinition.AnalysisDTO.DeploymentVerificationDTO.builder().enabled(true).build());
    metricDefinitions.get(0).setQueryType(HealthSourceQueryType.SERVICE_BASED);
    assertThatThrownBy(customHealthCVConfig::validateParams)
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Service based queries can only be used for live monitoring and service level indicators.");
  }

  @Test
  @Owner(developers = ANJAN)
  @Category(UnitTests.class)
  public void testValidateParams_whenThereAreDuplicateMetricDefinitions() {
    CustomHealthCVConfig.MetricDefinition metricDefinition =
        CustomHealthCVConfig.MetricDefinition.builder()
            .method(CustomHealthMethod.GET)
            .queryType(HealthSourceQueryType.HOST_BASED)
            .metricResponseMapping(responseMapping)
            .metricName("metric_1")
            .analysis(
                HealthSourceMetricDefinition.AnalysisDTO.builder()
                    .deploymentVerification(HealthSourceMetricDefinition.AnalysisDTO.DeploymentVerificationDTO.builder()
                                                .enabled(true)
                                                .build())
                    .build())
            .riskProfile(RiskProfile.builder().build())
            .sli(HealthSourceMetricDefinition.SLIDTO.builder().enabled(false).build())
            .urlPath("https://dd.com")
            .build();
    metricDefinitions.add(metricDefinition);

    assertThatThrownBy(customHealthCVConfig::validateParams)
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Duplicate group name (group1) and metric name (metric_1) combination present.");
  }
}

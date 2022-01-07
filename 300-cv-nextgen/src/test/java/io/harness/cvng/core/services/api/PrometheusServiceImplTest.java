/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.api;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.client.RequestExecutor;
import io.harness.cvng.client.VerificationManagerClient;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.delegate.beans.connector.prometheusconnector.PrometheusConnectorDTO;
import io.harness.rule.Owner;
import io.harness.serializer.JsonUtils;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class PrometheusServiceImplTest extends CvNextGenTestBase {
  @Inject private PrometheusService prometheusService;
  @Inject private OnboardingService onboardingService;
  @Mock private VerificationManagerClient verificationManagerClient;
  @Mock private NextGenService nextGenService;
  @Mock private VerificationManagerService verificationManagerService;
  @Mock private RequestExecutor requestExecutor;
  private String accountId;
  private String connectorIdentifier;

  @Before
  public void setup() throws IllegalAccessException {
    accountId = generateUuid();
    connectorIdentifier = generateUuid();

    FieldUtils.writeField(prometheusService, "onboardingService", onboardingService, true);
    FieldUtils.writeField(onboardingService, "nextGenService", nextGenService, true);
    FieldUtils.writeField(onboardingService, "verificationManagerService", verificationManagerService, true);

    when(nextGenService.get(anyString(), anyString(), anyString(), anyString()))
        .then(invocation
            -> Optional.of(
                ConnectorInfoDTO.builder().connectorConfig(PrometheusConnectorDTO.builder().build()).build()));
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetMetricNames() {
    List<String> metricNames = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      metricNames.add("metric - " + i);
    }
    when(verificationManagerService.getDataCollectionResponse(
             anyString(), anyString(), anyString(), any(DataCollectionRequest.class)))
        .thenReturn(JsonUtils.asJson(metricNames));

    List<String> metrics =
        prometheusService.getMetricNames(accountId, connectorIdentifier, generateUuid(), generateUuid(), "");

    assertThat(metrics).isNotEmpty();
    assertThat(metrics).hasSameElementsAs(metricNames);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetLabelNames() {
    List<String> labelNames = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      labelNames.add("label - " + i);
    }
    when(verificationManagerService.getDataCollectionResponse(
             anyString(), anyString(), anyString(), any(DataCollectionRequest.class)))
        .thenReturn(JsonUtils.asJson(labelNames));

    List<String> metrics =
        prometheusService.getLabelNames(accountId, connectorIdentifier, generateUuid(), generateUuid(), "");

    assertThat(metrics).isNotEmpty();
    assertThat(metrics).hasSameElementsAs(labelNames);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetLabelValues() {
    List<String> values = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      values.add("value - " + i);
    }
    when(verificationManagerService.getDataCollectionResponse(
             anyString(), anyString(), anyString(), any(DataCollectionRequest.class)))
        .thenReturn(JsonUtils.asJson(values));

    List<String> metrics = prometheusService.getLabelValues(
        accountId, connectorIdentifier, generateUuid(), generateUuid(), "labelName", "");

    assertThat(metrics).isNotEmpty();
    assertThat(metrics).hasSameElementsAs(values);
  }
}

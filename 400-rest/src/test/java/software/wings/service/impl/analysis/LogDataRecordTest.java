/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.analysis;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.SOWMYA;

import static software.wings.service.intfc.analysis.ClusterLevel.H0;
import static software.wings.service.intfc.analysis.ClusterLevel.L0;
import static software.wings.sm.StateType.ELK;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.StateType;

import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class LogDataRecordTest extends WingsBaseTest {
  StateType stateType;
  String applicationId;
  String cvConfigId;
  String stateExecutionId;
  String workflowId;
  String workflowExecutionId;
  String serviceId;
  ClusterLevel clusterLevel;
  ClusterLevel heartbeat;
  List<LogElement> logElements;
  String accountId;

  @Before
  public void setUp() {
    stateType = ELK;
    applicationId = generateUuid();
    cvConfigId = generateUuid();
    stateExecutionId = generateUuid();
    workflowId = generateUuid();
    workflowExecutionId = generateUuid();
    serviceId = generateUuid();
    clusterLevel = L0;
    heartbeat = H0;
    logElements = Collections.singletonList(
        LogElement.builder().host(generateUuid()).logMessage(generateUuid()).clusterLabel("0").build());
    accountId = generateUuid();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testGenerateDataRecords() {
    List<LogDataRecord> logDataRecords = LogDataRecord.generateDataRecords(stateType, applicationId, cvConfigId,
        stateExecutionId, workflowId, workflowExecutionId, serviceId, clusterLevel, heartbeat, logElements, accountId);
    assertThat(logDataRecords.size()).isEqualTo(1);
    assertThat(logDataRecords.get(0).getAccountId()).isEqualTo(accountId);
    assertThat(logDataRecords.get(0).getHost()).isEqualTo(logElements.get(0).getHost());
    assertThat(logDataRecords.get(0).getQuery()).isEqualTo(logElements.get(0).getQuery());
    assertThat(logDataRecords.get(0).getClusterLevel()).isEqualTo(clusterLevel);
  }
}

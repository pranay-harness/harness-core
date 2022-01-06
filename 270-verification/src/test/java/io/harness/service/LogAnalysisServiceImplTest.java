/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.SOWMYA;

import static software.wings.common.VerificationConstants.DUMMY_HOST_NAME;
import static software.wings.service.intfc.analysis.ClusterLevel.H0;
import static software.wings.service.intfc.analysis.ClusterLevel.H1;
import static software.wings.service.intfc.analysis.ClusterLevel.L0;
import static software.wings.service.intfc.analysis.ClusterLevel.L1;
import static software.wings.service.intfc.analysis.ClusterLevel.L2;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.VerificationBase;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.service.intfc.LogAnalysisService;

import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogDataRecord.LogDataRecordKeys;
import software.wings.verification.log.ElkCVConfiguration;
import software.wings.verification.log.LogsCVConfiguration;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class LogAnalysisServiceImplTest extends VerificationBase {
  @Inject private LogAnalysisService logAnalysisService;
  @Inject private WingsPersistence wingsPersistence;

  private String cvConfigId;
  private String appId;
  private int logCollectionMinute = 24312;
  private String host1 = "host1";
  private String host2 = "host2";

  @Before
  public void setUp() {
    cvConfigId = generateUuid();
    appId = generateUuid();
    LogDataRecord logDataRecord = LogDataRecord.builder()
                                      .clusterLevel(L0)
                                      .host(host1)
                                      .cvConfigId(cvConfigId)
                                      .logCollectionMinute(logCollectionMinute)
                                      .build();
    wingsPersistence.save(logDataRecord);
    logDataRecord = LogDataRecord.builder()
                        .clusterLevel(L0)
                        .host(host2)
                        .cvConfigId(cvConfigId)
                        .logCollectionMinute(logCollectionMinute)
                        .build();
    wingsPersistence.save(logDataRecord);
    logDataRecord = LogDataRecord.builder()
                        .clusterLevel(H0)
                        .host(DUMMY_HOST_NAME)
                        .cvConfigId(cvConfigId)
                        .logCollectionMinute(logCollectionMinute)
                        .build();
    wingsPersistence.save(logDataRecord);

    LogsCVConfiguration logsCVConfiguration = ElkCVConfiguration.builder().build();
    logsCVConfiguration.setUuid(cvConfigId);
    wingsPersistence.save(logsCVConfiguration);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testSaveClusteredLogData_L1Cluster() {
    List<LogDataRecord> dataRecords = wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority).asList();
    assertThat(dataRecords.size()).isEqualTo(3);
    boolean result =
        logAnalysisService.saveClusteredLogData(appId, cvConfigId, L1, logCollectionMinute, host1, new ArrayList<>());
    assertThat(result).isTrue();
    dataRecords = wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority).asList();
    assertThat(dataRecords.size()).isEqualTo(2);
    result =
        logAnalysisService.saveClusteredLogData(appId, cvConfigId, L1, logCollectionMinute, host2, new ArrayList<>());
    assertThat(result).isTrue();
    dataRecords = wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority).asList();
    assertThat(dataRecords.size()).isEqualTo(1);
    assertThat(dataRecords.get(0).getClusterLevel()).isEqualTo(H1);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetHostsForMinute() {
    Set<String> evenHosts = Sets.newHashSet("host1", "host2", "host3");
    Set<String> oddHosts = Sets.newHashSet("host4", "host5");
    int numOfMinutes = 20;
    for (int i = 0; i < numOfMinutes; i++) {
      wingsPersistence.save(LogDataRecord.builder()
                                .cvConfigId(cvConfigId)
                                .clusterLevel(L2)
                                .logCollectionMinute(i)
                                .host(generateUuid())
                                .logMessage(generateUuid())
                                .build());
      if (i % 2 == 0) {
        for (String host : evenHosts) {
          wingsPersistence.save(LogDataRecord.builder()
                                    .cvConfigId(cvConfigId)
                                    .clusterLevel(L1)
                                    .logCollectionMinute(i)
                                    .host(host)
                                    .logMessage(generateUuid())
                                    .build());
        }
      } else {
        for (String host : oddHosts) {
          wingsPersistence.save(LogDataRecord.builder()
                                    .cvConfigId(cvConfigId)
                                    .clusterLevel(L1)
                                    .logCollectionMinute(i)
                                    .host(host)
                                    .logMessage(generateUuid())
                                    .build());
        }
      }
    }

    for (int i = 0; i < numOfMinutes; i++) {
      assertThat(logAnalysisService.getHostsForMinute(appId, LogDataRecordKeys.cvConfigId, cvConfigId, i, L1))
          .isEqualTo(i % 2 == 0 ? evenHosts : oddHosts);
    }
  }
}

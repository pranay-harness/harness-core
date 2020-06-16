package io.harness.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.SOWMYA;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.common.VerificationConstants.DUMMY_HOST_NAME;
import static software.wings.service.intfc.analysis.ClusterLevel.H0;
import static software.wings.service.intfc.analysis.ClusterLevel.H1;
import static software.wings.service.intfc.analysis.ClusterLevel.L0;
import static software.wings.service.intfc.analysis.ClusterLevel.L1;

import com.google.inject.Inject;

import io.harness.VerificationBaseTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.service.intfc.LogAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.verification.log.ElkCVConfiguration;
import software.wings.verification.log.LogsCVConfiguration;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class LogAnalysisServiceImplTest extends VerificationBaseTest {
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
}
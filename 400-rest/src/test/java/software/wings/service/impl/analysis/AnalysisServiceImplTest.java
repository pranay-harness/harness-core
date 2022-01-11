/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.analysis;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.SOWMYA;

import static software.wings.service.impl.analysis.LogMLAnalysisStatus.LE_ANALYSIS_COMPLETE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineExperimentalAnalysisTask;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.sm.StateType;

import com.google.inject.Inject;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AnalysisServiceImplTest extends WingsBaseTest {
  @Inject private AnalysisService analysisService;
  @Inject private HPersistence persistence;

  private StateType stateType = StateType.ELK;
  private String appId;
  private String stateExecutionId;
  private String query;
  private String message;
  private String accountId;

  @Before
  public void setUp() throws IllegalAccessException {
    FieldUtils.writeField(analysisService, "wingsPersistence", persistence, true);

    appId = generateUuid();
    stateExecutionId = generateUuid();
    query = generateUuid();
    message = generateUuid();
    accountId = generateUuid();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCreateAndSaveSummary() {
    analysisService.createAndSaveSummary(stateType, appId, stateExecutionId, query, message, accountId);

    LogMLAnalysisRecord analysisRecord = persistence.createQuery(LogMLAnalysisRecord.class, excludeAuthority).get();

    assertThat(analysisRecord).isNotNull();
    assertThat(analysisRecord.getStateType()).isEqualTo(stateType);
    assertThat(analysisRecord.getStateExecutionId()).isEqualTo(stateExecutionId);
    assertThat(analysisRecord.getQuery()).isEqualTo(query);
    assertThat(analysisRecord.getAnalysisSummaryMessage()).isEqualTo(message);
    assertThat(analysisRecord.getAccountId()).isEqualTo(accountId);
    assertThat(analysisRecord.getAnalysisStatus()).isEqualTo(LE_ANALYSIS_COMPLETE);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCleanUpForLogRetry() {
    LogDataRecord logDataRecord = LogDataRecord.builder().stateExecutionId(stateExecutionId).build();
    persistence.save(logDataRecord);
    LogMLAnalysisRecord logMLAnalysisRecord = LogMLAnalysisRecord.builder().stateExecutionId(stateExecutionId).build();
    persistence.save(logMLAnalysisRecord);
    LearningEngineAnalysisTask analysisTask =
        LearningEngineAnalysisTask.builder().state_execution_id(stateExecutionId).build();
    persistence.save(analysisTask);
    LearningEngineExperimentalAnalysisTask experimentalAnalysisTask =
        LearningEngineExperimentalAnalysisTask.builder().state_execution_id(stateExecutionId).build();
    persistence.save(experimentalAnalysisTask);

    analysisService.cleanUpForLogRetry(stateExecutionId);

    LogDataRecord record = persistence.createQuery(LogDataRecord.class, excludeAuthority).get();
    assertThat(record).isNull();
    LogMLAnalysisRecord mlAnalysisRecord = persistence.createQuery(LogMLAnalysisRecord.class, excludeAuthority).get();
    assertThat(mlAnalysisRecord).isNull();
    LearningEngineAnalysisTask task = persistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority).get();
    assertThat(task).isNull();
    LearningEngineExperimentalAnalysisTask expTask =
        persistence.createQuery(LearningEngineExperimentalAnalysisTask.class, excludeAuthority).get();
    assertThat(expTask).isNull();
  }
}

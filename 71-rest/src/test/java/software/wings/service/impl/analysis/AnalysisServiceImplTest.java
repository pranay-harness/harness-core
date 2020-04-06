package software.wings.service.impl.analysis;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.SOWMYA;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.service.impl.analysis.LogMLAnalysisStatus.LE_ANALYSIS_COMPLETE;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.sm.StateType;

public class AnalysisServiceImplTest extends WingsBaseTest {
  @Inject private AnalysisService analysisService;
  @Inject private WingsPersistence wingsPersistence;

  private StateType stateType = StateType.ELK;
  private String appId;
  private String stateExecutionId;
  private String query;
  private String message;
  private String accountId;

  @Before
  public void setUp() throws IllegalAccessException {
    FieldUtils.writeField(analysisService, "wingsPersistence", wingsPersistence, true);

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

    LogMLAnalysisRecord analysisRecord =
        wingsPersistence.createQuery(LogMLAnalysisRecord.class, excludeAuthority).get();

    assertThat(analysisRecord).isNotNull();
    assertThat(analysisRecord.getStateType()).isEqualTo(stateType);
    assertThat(analysisRecord.getStateExecutionId()).isEqualTo(stateExecutionId);
    assertThat(analysisRecord.getQuery()).isEqualTo(query);
    assertThat(analysisRecord.getAnalysisSummaryMessage()).isEqualTo(message);
    assertThat(analysisRecord.getAccountId()).isEqualTo(accountId);
    assertThat(analysisRecord.getAnalysisStatus()).isEqualTo(LE_ANALYSIS_COMPLETE);
  }
}
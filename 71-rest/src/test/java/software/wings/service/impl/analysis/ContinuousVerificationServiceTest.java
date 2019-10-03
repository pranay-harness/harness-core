package software.wings.service.impl.analysis;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateExecutionInstance.Builder;
import software.wings.sm.StateExecutionInstance.StateExecutionInstanceKeys;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.util.HashMap;
import java.util.Map;

public class ContinuousVerificationServiceTest extends WingsBaseTest {
  @Inject private ContinuousVerificationService continuousVerificationService;

  @Test
  @Category(UnitTests.class)
  public void testGetVerificationStateExecutionData() {
    assertThat(continuousVerificationService.getVerificationStateExecutionData(generateUuid())).isNull();
    String stateExecutionId = wingsPersistence.save(Builder.aStateExecutionInstance().build());
    assertThat(continuousVerificationService.getVerificationStateExecutionData(stateExecutionId)).isNull();

    final String displayName = "new relic";
    Map<String, StateExecutionData> stateExecutionMap = new HashMap<>();
    final VerificationStateAnalysisExecutionData verificationStateAnalysisExecutionData =
        VerificationStateAnalysisExecutionData.builder()
            .stateExecutionInstanceId(stateExecutionId)
            .canaryNewHostNames(Sets.newHashSet("host1", "host2"))
            .lastExecutionNodes(Sets.newHashSet("host3", "host4"))
            .query(generateUuid())
            .baselineExecutionId(generateUuid())
            .mlAnalysisType(MLAnalysisType.TIME_SERIES)
            .timeDuration(10)
            .appId(generateUuid())
            .correlationId(generateUuid())
            .analysisMinute(5)
            .build();
    stateExecutionMap.put(displayName, verificationStateAnalysisExecutionData);

    wingsPersistence.updateField(StateExecutionInstance.class, stateExecutionId,
        StateExecutionInstanceKeys.stateExecutionMap, stateExecutionMap);
    wingsPersistence.updateField(
        StateExecutionInstance.class, stateExecutionId, StateExecutionInstanceKeys.displayName, displayName);
    final VerificationStateAnalysisExecutionData verificationStateExecutionData =
        continuousVerificationService.getVerificationStateExecutionData(stateExecutionId);
    assertThat(verificationStateExecutionData).isNotNull();
    assertThat(verificationStateExecutionData.getStateExecutionInstanceId())
        .isEqualTo(verificationStateAnalysisExecutionData.getStateExecutionInstanceId());
    assertThat(verificationStateExecutionData.getCanaryNewHostNames())
        .isEqualTo(verificationStateAnalysisExecutionData.getCanaryNewHostNames());
    assertThat(verificationStateExecutionData.getLastExecutionNodes())
        .isEqualTo(verificationStateAnalysisExecutionData.getLastExecutionNodes());
    assertThat(verificationStateExecutionData.getQuery()).isEqualTo(verificationStateAnalysisExecutionData.getQuery());
    assertThat(verificationStateExecutionData.getBaselineExecutionId())
        .isEqualTo(verificationStateAnalysisExecutionData.getBaselineExecutionId());
    assertThat(verificationStateExecutionData.getMlAnalysisType())
        .isEqualTo(verificationStateAnalysisExecutionData.getMlAnalysisType());
    assertThat(verificationStateExecutionData.getTimeDuration())
        .isEqualTo(verificationStateAnalysisExecutionData.getTimeDuration());
    assertThat(verificationStateExecutionData.getAnalysisMinute())
        .isEqualTo(verificationStateAnalysisExecutionData.getAnalysisMinute());
  }
}

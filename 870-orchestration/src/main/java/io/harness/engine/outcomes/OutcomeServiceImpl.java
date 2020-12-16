package io.harness.engine.outcomes;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.pms.data.PmsOutcomeService;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.resolver.outcome.mapper.PmsOutcomeMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.NonNull;

@OwnedBy(CDC)
@Redesign
@Singleton
public class OutcomeServiceImpl implements OutcomeService {
  @Inject private PmsOutcomeService pmsOutcomeService;

  @Override
  public String consume(Ambiance ambiance, String name, Outcome value, String groupName) {
    return pmsOutcomeService.consume(ambiance, name, PmsOutcomeMapper.convertOutcomeValueToJson(value), groupName);
  }

  @Override
  public Outcome resolve(Ambiance ambiance, RefObject refObject) {
    String json = pmsOutcomeService.resolve(ambiance, refObject);
    return PmsOutcomeMapper.convertJsonToOutcome(json);
  }

  @Override
  public List<Outcome> fetchOutcomes(List<String> outcomeInstanceIds) {
    List<String> outcomesAsJsonList = pmsOutcomeService.fetchOutcomes(outcomeInstanceIds);
    return PmsOutcomeMapper.convertJsonToOutcome(outcomesAsJsonList);
  }

  @Override
  public Outcome fetchOutcome(@NonNull String outcomeInstanceId) {
    String outcomeJson = pmsOutcomeService.fetchOutcome(outcomeInstanceId);
    return PmsOutcomeMapper.convertJsonToOutcome(outcomeJson);
  }

  @Override
  public List<Outcome> findAllByRuntimeId(String planExecutionId, String runtimeId) {
    List<String> allByRuntimeIdJsonList = pmsOutcomeService.findAllByRuntimeId(planExecutionId, runtimeId);
    return PmsOutcomeMapper.convertJsonToOutcome(allByRuntimeIdJsonList);
  }
}

package io.harness.pms.sdk.core.adviser.marksuccess;

import io.harness.pms.contracts.advisers.AdviseType;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.advisers.MarkSuccessAdvise;
import io.harness.pms.sdk.core.adviser.Adviser;
import io.harness.pms.sdk.core.adviser.AdvisingEvent;
import io.harness.pms.sdk.core.adviser.OrchestrationAdviserTypes;
import io.harness.serializer.KryoSerializer;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import javax.validation.constraints.NotNull;

public class OnMarkSuccessAdviser implements Adviser {
  public static final AdviserType ADVISER_TYPE =
      AdviserType.newBuilder().setType(OrchestrationAdviserTypes.MARK_SUCCESS.name()).build();

  @Inject KryoSerializer kryoSerializer;

  @Override
  public AdviserResponse onAdviseEvent(AdvisingEvent advisingEvent) {
    OnMarkSuccessAdviserParameters parameters = extractParameters(advisingEvent);
    return AdviserResponse.newBuilder()
        .setMarkSuccessAdvise(MarkSuccessAdvise.newBuilder().setNextNodeId(parameters.getNextNodeId()).build())
        .setType(AdviseType.MARK_SUCCESS)
        .build();
  }

  @Override
  public boolean canAdvise(AdvisingEvent advisingEvent) {
    return true;
  }

  @NotNull
  private OnMarkSuccessAdviserParameters extractParameters(AdvisingEvent advisingEvent) {
    return (OnMarkSuccessAdviserParameters) Preconditions.checkNotNull(
        kryoSerializer.asObject(advisingEvent.getAdviserParameters()));
  }
}

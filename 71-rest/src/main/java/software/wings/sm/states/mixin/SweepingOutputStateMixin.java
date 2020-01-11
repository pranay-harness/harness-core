package software.wings.sm.states.mixin;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.beans.SweepingOutputInstance;
import io.harness.serializer.KryoUtils;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContext;

public interface SweepingOutputStateMixin {
  String getSweepingOutputName();

  SweepingOutputInstance.Scope getSweepingOutputScope();

  default void handleSweepingOutput(
      SweepingOutputService sweepingOutputService, ExecutionContext context, Object data) {
    if (isEmpty(getSweepingOutputName())) {
      return;
    }

    final String renderedOutputName = context.renderExpression(getSweepingOutputName());

    final SweepingOutputInstance sweepingOutputInstance = context.prepareSweepingOutputBuilder(getSweepingOutputScope())
                                                              .name(renderedOutputName)
                                                              .output(KryoUtils.asDeflatedBytes(data))
                                                              .build();

    sweepingOutputService.save(sweepingOutputInstance);
  }
}
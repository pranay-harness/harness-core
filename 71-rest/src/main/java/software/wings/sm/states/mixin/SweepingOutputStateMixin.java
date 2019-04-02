package software.wings.sm.states.mixin;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.serializer.KryoUtils;
import software.wings.beans.SweepingOutput;
import software.wings.service.intfc.SweepingOutputService;
import software.wings.sm.ExecutionContext;

public interface SweepingOutputStateMixin {
  String getSweepingOutputName();

  SweepingOutput.Scope getSweepingOutputScope();

  default void handleSweepingOutput(
      SweepingOutputService sweepingOutputService, ExecutionContext context, Object data) {
    if (isEmpty(getSweepingOutputName())) {
      return;
    }

    final String renderedOutputName = context.renderExpression(getSweepingOutputName());

    final SweepingOutput sweepingOutput = context.prepareSweepingOutputBuilder(getSweepingOutputScope())
                                              .name(renderedOutputName)
                                              .output(KryoUtils.asDeflatedBytes(data))
                                              .build();

    sweepingOutputService.save(sweepingOutput);
  }
}
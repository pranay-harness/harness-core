package software.wings.sm.states.mixin;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.HasPredicate.hasNone;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SweepingOutputInstance;
import io.harness.serializer.KryoSerializer;

import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContext;

@OwnedBy(CDC)
public interface SweepingOutputStateMixin {
  String getSweepingOutputName();

  SweepingOutputInstance.Scope getSweepingOutputScope();

  KryoSerializer getKryoSerializer();

  default void handleSweepingOutput(
      SweepingOutputService sweepingOutputService, ExecutionContext context, Object data) {
    if (hasNone(getSweepingOutputName())) {
      return;
    }

    final String renderedOutputName = context.renderExpression(getSweepingOutputName());

    final SweepingOutputInstance sweepingOutputInstance = context.prepareSweepingOutputBuilder(getSweepingOutputScope())
                                                              .name(renderedOutputName)
                                                              .output(getKryoSerializer().asDeflatedBytes(data))
                                                              .build();

    sweepingOutputService.save(sweepingOutputInstance);
  }
}

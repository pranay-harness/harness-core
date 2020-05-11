package software.wings.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SweepingOutputInstance;
import io.harness.expression.LateBindingValue;
import io.harness.serializer.KryoUtils;
import lombok.Builder;
import software.wings.service.intfc.sweepingoutput.SweepingOutputInquiry;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;

@OwnedBy(CDC)
@Builder
public class SweepingOutputValue implements LateBindingValue {
  private SweepingOutputService sweepingOutputService;

  private SweepingOutputInquiry sweepingOutputInquiry;

  @Override
  public Object bind() {
    SweepingOutputInstance sweepingOutputInstance = sweepingOutputService.find(sweepingOutputInquiry);
    if (sweepingOutputInstance == null) {
      return null;
    }

    if (sweepingOutputInstance.getValue() != null) {
      return sweepingOutputInstance.getValue();
    }

    return KryoUtils.asInflatedObject(sweepingOutputInstance.getOutput());
  }
}

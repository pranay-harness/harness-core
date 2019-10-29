package software.wings.expression;

import io.harness.beans.SweepingOutput;
import io.harness.expression.LateBindingValue;
import io.harness.serializer.KryoUtils;
import lombok.Builder;
import software.wings.service.intfc.SweepingOutputService;
import software.wings.service.intfc.SweepingOutputService.SweepingOutputInquiry;

@Builder
public class SweepingOutputValue implements LateBindingValue {
  private SweepingOutputService sweepingOutputService;

  private SweepingOutputInquiry sweepingOutputInquiry;

  @Override
  public Object bind() {
    SweepingOutput sweepingOutput = sweepingOutputService.find(sweepingOutputInquiry);
    if (sweepingOutput == null) {
      return null;
    }
    return KryoUtils.asInflatedObject(sweepingOutput.getOutput());
  }
}

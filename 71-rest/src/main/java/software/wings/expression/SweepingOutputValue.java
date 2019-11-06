package software.wings.expression;

import io.harness.beans.SweepingOutputInstance;
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
    SweepingOutputInstance sweepingOutputInstance = sweepingOutputService.find(sweepingOutputInquiry);
    if (sweepingOutputInstance == null) {
      return null;
    }
    return KryoUtils.asInflatedObject(sweepingOutputInstance.getOutput());
  }
}

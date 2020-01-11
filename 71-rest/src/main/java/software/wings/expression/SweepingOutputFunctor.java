package software.wings.expression;

import static java.lang.String.format;

import io.harness.beans.SweepingOutputInstance;
import io.harness.expression.LateBindingMap;
import io.harness.serializer.KryoUtils;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import software.wings.exception.SweepingOutputException;
import software.wings.service.intfc.sweepingoutput.SweepingOutputInquiry.SweepingOutputInquiryBuilder;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;

@Value
@Builder
@EqualsAndHashCode(callSuper = true)
public class SweepingOutputFunctor extends LateBindingMap {
  private transient SweepingOutputInquiryBuilder sweepingOutputInquiryBuilder;

  private transient SweepingOutputService sweepingOutputService;

  public synchronized Object output(String name) {
    SweepingOutputInstance sweepingOutputInstance =
        sweepingOutputService.find(sweepingOutputInquiryBuilder.name(name).build());
    if (sweepingOutputInstance == null) {
      throw new SweepingOutputException(format("Missing sweeping output %s", name));
    }

    if (sweepingOutputInstance.getValue() != null) {
      return sweepingOutputInstance.getValue();
    }

    return KryoUtils.asInflatedObject(sweepingOutputInstance.getOutput());
  }

  @Override
  public synchronized Object get(Object key) {
    return output((String) key);
  }
}

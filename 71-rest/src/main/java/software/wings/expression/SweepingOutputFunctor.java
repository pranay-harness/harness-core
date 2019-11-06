package software.wings.expression;

import static java.lang.String.format;

import io.harness.beans.SweepingOutputInstance;
import io.harness.expression.LateBindingMap;
import io.harness.serializer.KryoUtils;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import software.wings.exception.SweepingOutputException;
import software.wings.service.intfc.SweepingOutputService;
import software.wings.service.intfc.SweepingOutputService.SweepingOutputInquiry.SweepingOutputInquiryBuilder;

@Value
@Builder
@EqualsAndHashCode(callSuper = true)
public class SweepingOutputFunctor extends LateBindingMap {
  SweepingOutputInquiryBuilder sweepingOutputInquiryBuilder;

  private transient SweepingOutputService sweepingOutputService;

  public Object output(String name) {
    SweepingOutputInstance sweepingOutputInstance =
        sweepingOutputService.find(sweepingOutputInquiryBuilder.name(name).build());
    if (sweepingOutputInstance == null) {
      throw new SweepingOutputException(format("Missing sweeping output %s", name));
    }
    return KryoUtils.asInflatedObject(sweepingOutputInstance.getOutput());
  }

  @Override
  public Object get(Object key) {
    return output((String) key);
  }
}

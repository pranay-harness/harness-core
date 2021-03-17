package software.wings.beans.alert;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@FieldNameConstants(innerTypeName = "AlertReconciliationKeys")
@TargetModule(Module._955_ALERT_BEANS)
public class AlertReconciliation {
  @Getter private boolean needed;
  @Getter @Setter private Long nextIteration;

  public static final AlertReconciliation noop = new AlertReconciliation();
}

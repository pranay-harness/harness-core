package software.wings.beans.alert;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@FieldNameConstants(innerTypeName = "AlertReconciliationKeys")
public class AlertReconciliation {
  @Getter private boolean needed;
  @Getter @Setter private Long nextIteration;

  public static final AlertReconciliation noop = new AlertReconciliation();
}

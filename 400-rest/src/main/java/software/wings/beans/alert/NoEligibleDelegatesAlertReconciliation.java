package software.wings.beans.alert;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@FieldNameConstants(innerTypeName = "NoEligibleDelegatesAlertReconciliationKeys")
@Value
@Builder
public class NoEligibleDelegatesAlertReconciliation extends AlertReconciliation {
  private List<String> delegates;
}

package software.wings.beans.alert;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@FieldNameConstants(innerTypeName = "NoEligibleDelegatesAlertReconciliationKeys")
@Value
@Builder
public class NoEligibleDelegatesAlertReconciliation extends AlertReconciliation {
  private List<String> delegates;
}

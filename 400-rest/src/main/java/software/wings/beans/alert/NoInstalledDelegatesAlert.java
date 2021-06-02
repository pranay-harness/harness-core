package software.wings.beans.alert;

import io.harness.alert.AlertData;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(HarnessModule._920_DELEGATE_SERVICE_BEANS)
public class NoInstalledDelegatesAlert implements AlertData {
  private String accountId;

  @Override
  public boolean matches(AlertData alertData) {
    return accountId.equals(((NoInstalledDelegatesAlert) alertData).getAccountId());
  }

  @Override
  public String buildTitle() {
    return "No installed delegates found";
  }
}

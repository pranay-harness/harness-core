package software.wings.beans.alert;

import com.google.inject.Injector;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NoActiveDelegatesAlert implements AlertData {
  private String accountId;

  @Override
  public boolean matches(AlertData alertData, Injector injector) {
    return accountId.equals(((NoActiveDelegatesAlert) alertData).getAccountId());
  }

  @Override
  public String buildTitle(Injector injector) {
    return "No delegates are available";
  }
}

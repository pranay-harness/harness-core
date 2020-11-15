package software.wings.service.intfc.ce;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public interface CeAccountExpirationChecker {
  void checkIsCeEnabled(String accountId);
}

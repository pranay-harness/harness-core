package software.wings.licensing;

import io.harness.exception.WingsException;
import software.wings.beans.Account;

/**
 * Created by peeyushaggarwal on 3/22/17.
 */
public interface LicenseManager {
  void validateLicense(String accountId, String operation) throws WingsException;
  void setLicense(Account account);
}

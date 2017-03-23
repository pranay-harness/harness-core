package software.wings.licensing;

import software.wings.beans.Account;
import software.wings.exception.WingsException;

/**
 * Created by peeyushaggarwal on 3/22/17.
 */
public interface LicenseManager {
  void validateLicense(String accountId, String operation) throws WingsException;
  void setLicense(Account account);
}

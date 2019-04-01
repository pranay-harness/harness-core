package software.wings.licensing;

import io.harness.exception.WingsException;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Account;
import software.wings.beans.LicenseInfo;

import java.util.List;

/**
 *
 * @author rktummala on 11/10/18
 */
public interface LicenseService {
  void checkForLicenseExpiry();

  Account addLicenseInfo(Account account);

  boolean updateAccountLicense(@NotEmpty String accountId, LicenseInfo licenseInfo);

  Account updateAccountSalesContacts(@NotEmpty String accountId, List<String> salesContacts);

  Account decryptLicenseInfo(Account account, boolean setExpiry);

  String generateLicense(LicenseInfo licenseInfo);

  void updateAccountLicenseForOnPrem(String encryptedLicenseInfoBase64String);

  boolean isAccountDeleted(String accountId);

  boolean isAccountExpired(String accountId);

  void validateLicense(String accountId, String operation) throws WingsException;

  void setLicense(Account account);
}

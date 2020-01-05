package software.wings.licensing;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Account;
import software.wings.beans.LicenseInfo;

import java.util.List;

public interface LicenseService {
  String LICENSE_INFO = "LICENSE_INFO";

  void checkForLicenseExpiry(Account account);

  Account addLicenseInfo(Account account);

  boolean updateAccountLicense(@NotEmpty String accountId, LicenseInfo licenseInfo);

  Account updateAccountSalesContacts(@NotEmpty String accountId, List<String> salesContacts);

  Account decryptLicenseInfo(Account account, boolean setExpiry);

  String generateLicense(LicenseInfo licenseInfo);

  void updateAccountLicenseForOnPrem(String encryptedLicenseInfoBase64String);

  boolean isAccountDeleted(String accountId);

  boolean isAccountExpired(String accountId);

  void validateLicense(String accountId, String operation);

  void setLicense(Account account);
}

package software.wings.licensing;

import static io.harness.eraro.ErrorCode.LICENSE_EXPIRED;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import software.wings.beans.Account;
import software.wings.beans.License;
import software.wings.beans.LicenseInfo;
import software.wings.service.intfc.AccountService;

import java.util.List;

/**
 * Created by peeyushaggarwal on 3/22/17.
 */
public class LicenseManagerImpl implements LicenseManager {
  @Inject private AccountService accountService;

  @Inject private LicenseProvider licenseProvider;

  @Override
  public void validateLicense(String accountId, String operation) throws WingsException {
    Account account = accountService.get(accountId);
    LicenseInfo licenseInfo = account.getLicenseInfo();
    if (licenseInfo == null) {
      throw new WingsException(LICENSE_EXPIRED);
    }

    if (licenseInfo.getExpiryTime() > 0 && System.currentTimeMillis() > licenseInfo.getExpiryTime()) {
      licenseProvider.get(account.getLicenseId());
      // throw new WingsException(LICENSE_NOT_ALLOWED);
    } else {
      throw new WingsException(LICENSE_EXPIRED);
    }
  }

  @Override
  public void setLicense(Account account) {
    List<License> licenseList = licenseProvider.getActiveLicenses();
    account.setLicenseId(licenseList.get(0).getUuid());
    if (account.getLicenseInfo() == null) {
      account.setLicenseInfo(new LicenseInfo());
    }

    account.getLicenseInfo().setExpiryTime(System.currentTimeMillis() + licenseList.get(0).getExpiryDuration());
  }
}

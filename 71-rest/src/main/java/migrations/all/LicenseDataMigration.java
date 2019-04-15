package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.common.Constants;
import software.wings.dl.WingsPersistence;
import software.wings.licensing.LicenseService;
import software.wings.service.impl.LicenseUtil;
import software.wings.service.intfc.AccountService;

/**
 * Migration script to update license info for all accounts.
 *
 * @author rktummala on 11/02/18
 */
@Slf4j
public class LicenseDataMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;
  @Inject private LicenseService licenseService;

  @Override
  public void migrate() {
    logger.info("LicenseMigration - Start - Updating license info for all accounts");
    Query<Account> accountsQuery = wingsPersistence.createQuery(Account.class, excludeAuthority);
    try (HIterator<Account> records = new HIterator<>(accountsQuery.fetch())) {
      while (records.hasNext()) {
        Account account = null;
        try {
          account = records.next();

          Account accountWithDecryptedLicenseInfo = licenseService.decryptLicenseInfo(account, false);
          LicenseInfo licenseInfo = accountWithDecryptedLicenseInfo.getLicenseInfo();
          if (licenseInfo == null) {
            continue;
          }

          String accountType = licenseInfo.getAccountType();

          if (!AccountType.isValid(accountType)) {
            logger.error(
                "LicenseMigration - Invalid accountType {} for account {}", accountType, account.getAccountName());
            continue;
          }

          long expiryTime = licenseInfo.getExpiryTime();
          int licenseUnits = licenseInfo.getLicenseUnits();

          switch (accountType) {
            case AccountType.PAID:
              if (expiryTime <= 0) {
                licenseInfo.setExpiryTime(LicenseUtil.getDefaultPaidExpiryTime());
              }

              if (licenseUnits <= 0) {
                licenseInfo.setLicenseUnits(Constants.DEFAULT_PAID_LICENSE_UNITS);
              }
              break;

            case AccountType.TRIAL:
              if (expiryTime <= 0) {
                licenseInfo.setExpiryTime(LicenseUtil.getDefaultTrialExpiryTime());
              }

              if (licenseUnits <= 0) {
                licenseInfo.setLicenseUnits(Constants.DEFAULT_TRIAL_LICENSE_UNITS);
              }
              break;

            case AccountType.FREE:
              if (expiryTime <= 0) {
                licenseInfo.setExpiryTime(-1L);
              }

              if (licenseUnits <= 0) {
                licenseInfo.setLicenseUnits(Constants.DEFAULT_FREE_LICENSE_UNITS);
              }
              break;

            default:
              logger.error("Unsupported account type {} for account {}", accountType, account.getAccountName());
              break;
          }

          licenseService.updateAccountLicense(account.getUuid(), licenseInfo);
          logger.info("LicenseMigration - Updated license info for account {}", account.getAccountName());
        } catch (Exception ex) {
          logger.error("LicenseMigration - Error while updating license info for account: {}",
              account != null ? account.getAccountName() : "", ex);
        }
      }

      logger.info("LicenseMigration - Done - Updating license info for all accounts");
    } catch (Exception ex) {
      logger.error("LicenseMigration - Failed - Updating license info for all accounts", ex);
    }
  }
}

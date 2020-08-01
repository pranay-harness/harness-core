package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.ccm.license.CeLicenseInfo;
import io.harness.ccm.license.CeLicenseType;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Account;
import software.wings.beans.Account.AccountKeys;
import software.wings.dl.WingsPersistence;
import software.wings.licensing.LicenseService;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
public class AddCeFullTrialLicenseToCurrentAccounts implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private LicenseService licenseService;

  @Override
  public void migrate() {
    logger.info("License Migration - Start adding CE Full Trial licenses for existing accounts");

    CeLicenseInfo fullTrialLicense = CeLicenseInfo.builder()
                                         .licenseType(CeLicenseType.FULL_TRIAL)
                                         .expiryTime(CeLicenseType.FULL_TRIAL.getDefaultExpiryTime())
                                         .build();

    Map<String, String> paidCeAccountExpiryMap = ImmutableMap.<String, String>builder()
                                                     .put("g9Nw8fnOSYGlr4H9QYWJww", "06/04/2021")
                                                     .put("R7OsqSbNQS69mq74kMNceQ", "05/15/2023")
                                                     .put("8VwWgE0WRK67_PWDpkooNA", "07/26/2021")
                                                     .put("BpYJcC5sR76ag3to4FbubQ", "09/30/2020")
                                                     .put("WhejVM7NTJe2fZ99Pdo2YA", "07/15/2021")
                                                     .put("hW63Ny6rQaaGsKkVjE0pJA", "08/20/2020")
                                                     .put("TlKfvX4wQNmRmxkZrPXEgQ", "10/30/2020")
                                                     .put("NVsV7gjbTZyA3CgSgXNOcg", "07/30/2023")
                                                     .build();

    Query<Account> accountsQuery = wingsPersistence.createQuery(Account.class, excludeAuthority)
                                       .field(AccountKeys.cloudCostEnabled)
                                       .equal(Boolean.TRUE);
    try (HIterator<Account> records = new HIterator<>(accountsQuery.fetch())) {
      while (records.hasNext()) {
        Account account = records.next();
        try {
          if (account.isCloudCostEnabled() && account.getCeLicenseInfo() == null) {
            if (paidCeAccountExpiryMap.containsKey(account.getUuid())) {
              CeLicenseInfo paidCeLicenseInfo =
                  CeLicenseInfo.builder()
                      .licenseType(CeLicenseType.PAID)
                      .expiryTime(getExpiryTime(paidCeAccountExpiryMap.get(account.getUuid())))
                      .build();
              licenseService.updateCeLicense(account.getUuid(), paidCeLicenseInfo);

            } else {
              licenseService.updateCeLicense(account.getUuid(), fullTrialLicense);
            }
          }
        } catch (Exception ex) {
          logger.error("Error while adding CE license for account {}", account.getUuid(), ex);
        }
      }
    }
    logger.info("License Migration - Completed adding CE Full Trial licenses for existing accounts");
  }

  private long getExpiryTime(String expiryDate) {
    return LocalDate.parse(expiryDate, DateTimeFormatter.ofPattern("MM/dd/yyyy"))
        .atStartOfDay()
        .toInstant(ZoneOffset.UTC)
        .toEpochMilli();
  }
}

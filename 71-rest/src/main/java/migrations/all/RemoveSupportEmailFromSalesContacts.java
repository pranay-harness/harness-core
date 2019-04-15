package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.licensing.LicenseService;

import java.util.List;

/**
 * Migration script to remove support@harness.io from salesContacts info from all accounts
 * @author rktummala on 01/14/19
 */
@Slf4j
public class RemoveSupportEmailFromSalesContacts implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private LicenseService licenseService;

  @Override
  public void migrate() {
    logger.info("RemoveSupportEmail - Start - Removing support email from salesContacts for all accounts");
    Query<Account> accountsQuery = wingsPersistence.createQuery(Account.class, excludeAuthority);
    try (HIterator<Account> records = new HIterator<>(accountsQuery.fetch())) {
      while (records.hasNext()) {
        Account account = null;
        try {
          account = records.next();
          List<String> salesContacts = account.getSalesContacts();
          if (isEmpty(salesContacts)) {
            continue;
          }

          boolean removed = salesContacts.remove("support@harness.io");

          if (!removed) {
            continue;
          }

          licenseService.updateAccountSalesContacts(account.getUuid(), salesContacts);
          logger.info("RemoveSupportEmail - Updated sales contacts for account {}", account.getUuid());
        } catch (Exception ex) {
          logger.error("RemoveSupportEmail - Error while updating sales contacts for account: {}",
              account != null ? account.getAccountName() : "", ex);
        }
      }

      logger.info("RemoveSupportEmail - Done - Removing support email from salesContacts");
    } catch (Exception ex) {
      logger.error("RemoveSupportEmail - Failed - Removing support email from salesContacts", ex);
    }
  }

  private String getListAsString(List<String> salesContacts) {
    StringBuilder sb = new StringBuilder();
    if (isEmpty(salesContacts)) {
      return null;
    }

    salesContacts.forEach(salesContact -> {
      sb.append(salesContact);
      sb.append(',');
    });

    return sb.substring(0, sb.length() - 1);
  }
}

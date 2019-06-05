package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Account;
import software.wings.beans.TechStack;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;

import java.util.HashSet;
import java.util.Set;

/**
 * Migration script to add dummy tech stack to all existing accounts. This will allow UI to show new trial experience
 * for only new account users.
 * @author rktummala on 06/04/19
 */
@Slf4j
public class SetDummyTechStackForOldAccounts implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;

  @Override
  public void migrate() {
    logger.info("SetDummyTechStack - Start");
    Query<Account> accountsQuery = wingsPersistence.createQuery(Account.class, excludeAuthority);
    try (HIterator<Account> records = new HIterator<>(accountsQuery.fetch())) {
      while (records.hasNext()) {
        Account account = null;
        try {
          account = records.next();
          Set<TechStack> techStacks = account.getTechStacks();
          if (isNotEmpty(techStacks)) {
            logger.info("SetDummyTechStack - skip for account {}", account.getUuid());
            continue;
          }

          techStacks = new HashSet<>();
          techStacks.add(TechStack.builder().technology("NONE").category("NONE").build());

          accountService.updateTechStacks(account.getUuid(), techStacks);
          logger.info("SetDummyTechStack - Updated dummy tech stacks for account {}", account.getUuid());
        } catch (Exception ex) {
          logger.error("SetDummyTechStack - Error while updating dummy tech stacks for account: {}",
              account != null ? account.getAccountName() : "", ex);
        }
      }

      logger.info("SetDummyTechStack - Done - Updated dummy tech stacks ");
    } catch (Exception ex) {
      logger.error("SetDummyTechStack - Failed - Updated dummy tech stacks ", ex);
    }
  }
}

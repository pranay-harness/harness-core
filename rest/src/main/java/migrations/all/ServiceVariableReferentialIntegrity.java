package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.Base.APP_ID_KEY;
import static software.wings.beans.Base.ID_KEY;

import com.google.inject.Inject;

import migrations.Migration;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.ServiceVariable;
import software.wings.dl.WingsPersistence;

import java.util.List;

/**
 * Created by brett on 5/1/18.
 */
public class ServiceVariableReferentialIntegrity implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(ServiceVariableReferentialIntegrity.class);

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    logger.info("Checking service variables for invalid parent references");

    UpdateOperations<ServiceVariable> updateOperations =
        wingsPersistence.createUpdateOperations(ServiceVariable.class).unset("parentServiceVariableId");

    List<Account> accounts = wingsPersistence.createQuery(Account.class, excludeAuthority).asList();
    logger.info("Checking {} accounts", accounts.size());
    for (Account account : accounts) {
      List<Application> apps =
          wingsPersistence.createQuery(Application.class).filter(ACCOUNT_ID_KEY, account.getUuid()).asList();
      logger.info("Checking {} applications in account {}", apps.size(), account.getAccountName());
      for (Application app : apps) {
        List<ServiceVariable> refVariables = wingsPersistence.createQuery(ServiceVariable.class)
                                                 .filter(APP_ID_KEY, app.getUuid())
                                                 .field("parentServiceVariableId")
                                                 .exists()
                                                 .asList();
        logger.info("  Checking {} variables in application {}", refVariables.size(), app.getName());
        for (ServiceVariable var : refVariables) {
          String parentId = var.getParentServiceVariableId();
          ServiceVariable parent = wingsPersistence.createQuery(ServiceVariable.class)
                                       .filter(APP_ID_KEY, app.getUuid())
                                       .filter(ID_KEY, parentId)
                                       .get();
          if (parent == null) {
            logger.info("    Clearing invalid parent reference in {}", var.getName());
            wingsPersistence.update(var, updateOperations);
          }
        }
      }
    }
  }
}

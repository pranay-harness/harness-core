package migrations.all;

import static io.harness.persistence.UuidAccess.ID_KEY;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import io.harness.persistence.HIterator;
import io.harness.persistence.ReadPref;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.Base;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;

/**
 *
 * @author rktummala on 02/26/19
 */
public class AddAccountIdToAppEntities implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(AddAccountIdToAppEntities.class);

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try (HIterator<Account> accounts =
             new HIterator<>(wingsPersistence.createQuery(Account.class).project(ID_KEY, true).fetch())) {
      while (accounts.hasNext()) {
        final Account account = accounts.next();
        bulkSetAccountId(account.getUuid(), Service.class);
        bulkSetAccountId(account.getUuid(), Environment.class);
        bulkSetAccountId(account.getUuid(), InfrastructureProvisioner.class);
        bulkSetAccountId(account.getUuid(), Workflow.class);
        bulkSetAccountId(account.getUuid(), Pipeline.class);
      }
    }
  }

  private <T extends Base> void bulkSetAccountId(String accountId, Class<T> clazz) {
    final DBCollection collection = wingsPersistence.getCollection(clazz, ReadPref.NORMAL);
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    int i = 1;
    try (HIterator<T> entities = new HIterator<>(
             wingsPersistence.createQuery(clazz).field("accountId").doesNotExist().project(ID_KEY, true).fetch())) {
      while (entities.hasNext()) {
        final T entity = entities.next();

        if (i % 1000 == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          logger.info("Entity:{} {} updated", clazz.getSimpleName(), i);
        }
        ++i;

        bulkWriteOperation
            .find(
                wingsPersistence.createQuery(WorkflowExecution.class).filter(ID_KEY, entity.getUuid()).getQueryObject())
            .updateOne(new BasicDBObject("$set", new BasicDBObject("accountId", accountId)));
      }
    }
    if (i % 1000 != 1) {
      bulkWriteOperation.execute();
    }
  }
}

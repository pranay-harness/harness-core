package migrations.all;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import io.harness.persistence.HIterator;
import io.harness.persistence.ReadPref;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class AddValidUntilToWorkflowExecution implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(AddValidUntilToWorkflowExecution.class);

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(WorkflowExecution.class, ReadPref.NORMAL);
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    int i = 1;
    try (HIterator<WorkflowExecution> workflowExecutions =
             new HIterator<>(wingsPersistence.createQuery(WorkflowExecution.class)
                                 .field("validUntil")
                                 .doesNotExist()
                                 .project(WorkflowExecution.CREATED_AT_KEY, true)
                                 .fetch())) {
      while (workflowExecutions.hasNext()) {
        final WorkflowExecution workflowExecution = workflowExecutions.next();
        final ZonedDateTime zonedDateTime =
            Instant.ofEpochMilli(workflowExecution.getCreatedAt()).atZone(ZoneOffset.UTC).plusMonths(6);

        if (i % 1000 == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          logger.info("WorkflowExecutions: {} updated", i);
        }
        ++i;

        bulkWriteOperation
            .find(wingsPersistence.createQuery(WorkflowExecution.class)
                      .filter(WorkflowExecution.ID_KEY, workflowExecution.getUuid())
                      .getQueryObject())
            .updateOne(new BasicDBObject(
                "$set", new BasicDBObject("validUntil", java.util.Date.from(zonedDateTime.toInstant()))));
      }
    }
    if (i % 1000 != 1) {
      bulkWriteOperation.execute();
    }
  }
}

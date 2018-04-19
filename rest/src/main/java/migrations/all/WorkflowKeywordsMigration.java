package migrations.all;

import static software.wings.beans.Base.ID_KEY;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import migrations.Migration;
import org.mongodb.morphia.query.MorphiaIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Environment;
import software.wings.beans.Workflow;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.WorkflowServiceHelper;
import software.wings.service.intfc.WorkflowService;

import java.util.List;

public class WorkflowKeywordsMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowKeywordsMigration.class);
  @Inject WingsPersistence wingsPersistence;
  @Inject WorkflowService workflowService;
  @Inject WorkflowServiceHelper workflowServiceHelper;

  public static final int BATCH_SIZE = 20;

  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection("workflows");
    MorphiaIterator<Workflow, Workflow> workflows = wingsPersistence.createQuery(Workflow.class).fetch();
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();
    int i = 1;
    try (DBCursor ignored = workflows.getCursor()) {
      while (workflows.hasNext()) {
        Workflow workflow = workflows.next();
        Environment environment = wingsPersistence.get(Environment.class, workflow.getAppId(), workflow.getEnvId());
        if (workflow.getEnvId() != null && environment == null) {
          logger.info("Workflow {} of appId {} does not have environment id {}", workflow.getName(),
              workflow.getAppId(), workflow.getEnvId());
          continue;
        }
        workflowService.loadOrchestrationWorkflow(workflow, workflow.getDefaultVersion());
        if (i % BATCH_SIZE == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          logger.info("Workflows: {} updated", i);
        }
        ++i;
        List<String> keywords = workflowServiceHelper.getKeywords(workflow);
        bulkWriteOperation
            .find(wingsPersistence.createQuery(Workflow.class).filter(ID_KEY, workflow.getUuid()).getQueryObject())
            .updateOne(new BasicDBObject("$set", new BasicDBObject("keywords", keywords)));
      }
    }
    if (i % BATCH_SIZE != 1) {
      bulkWriteOperation.execute();
    }
  }
}
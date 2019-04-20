package migrations.all;

import com.mongodb.DBObject;
import lombok.extern.slf4j.Slf4j;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;

/**
 * Created by rsingh on 5/20/18.
 */
@Slf4j
public class LearningEngineTaskGroupNameMigration extends AddFieldMigration {
  @Override
  protected org.slf4j.Logger getLogger() {
    return logger;
  }

  @Override
  protected String getCollectionName() {
    return "learningEngineAnalysisTask";
  }

  @Override
  protected Class getCollectionClass() {
    return LearningEngineAnalysisTask.class;
  }

  @Override
  protected String getFieldName() {
    return "group_name";
  }

  @Override
  protected Object getFieldValue(DBObject existingRecord) {
    return NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
  }
}

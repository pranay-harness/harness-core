package migrations.all;

import org.slf4j.LoggerFactory;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;

/**
 * Created by rsingh on 5/20/18.
 */
public class LearningEngineTaskGroupNameMigration extends AddFieldMigration {
  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(LearningEngineTaskGroupNameMigration.class);
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
  protected Object getFieldValue() {
    return NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
  }
}

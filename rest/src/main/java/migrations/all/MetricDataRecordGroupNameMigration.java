package migrations.all;

import org.slf4j.LoggerFactory;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;

/**
 * Created by rsingh on 5/20/18.
 */
public class MetricDataRecordGroupNameMigration extends AddFieldMigration {
  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MetricDataRecordGroupNameMigration.class);
  @Override
  protected org.slf4j.Logger getLogger() {
    return logger;
  }

  @Override
  protected String getCollectionName() {
    return "newRelicMetricRecords";
  }

  @Override
  protected Class getCollectionClass() {
    return NewRelicMetricDataRecord.class;
  }

  @Override
  protected String getFieldName() {
    return "groupName";
  }

  @Override
  protected Object getFieldValue() {
    return NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
  }
}

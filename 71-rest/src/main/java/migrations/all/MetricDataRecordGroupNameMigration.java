package migrations.all;

import com.mongodb.DBObject;
import lombok.extern.slf4j.Slf4j;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;

/**
 * Created by rsingh on 5/20/18.
 */
@Slf4j
public class MetricDataRecordGroupNameMigration extends AddFieldMigration {
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
  protected Object getFieldValue(DBObject existingRecord) {
    return NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
  }
}

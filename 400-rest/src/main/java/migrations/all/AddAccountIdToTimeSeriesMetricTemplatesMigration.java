package migrations.all;

import software.wings.service.impl.analysis.TimeSeriesMetricTemplates.TimeSeriesMetricTemplatesKeys;

public class AddAccountIdToTimeSeriesMetricTemplatesMigration extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "timeSeriesMetricTemplates";
  }

  @Override
  protected String getFieldName() {
    return TimeSeriesMetricTemplatesKeys.accountId;
  }
}

package io.harness.migrations.all;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.service.impl.analysis.TimeSeriesMetricTemplates.TimeSeriesMetricTemplatesKeys;

@TargetModule(Module._390_DB_MIGRATION)
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

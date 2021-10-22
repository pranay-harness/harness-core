package io.harness.cvng.core.entities;

import io.harness.cvng.beans.DataSourceType;

public class DatadogLogCVConfig extends MetricCVConfig {

    @Override
    protected void validateParams() {
    }

    @Override
    public DataSourceType getType() {
        return DataSourceType.DATADOG_LOG;
    }

    @Override
    public String getDataCollectionDsl() {
        return getMetricPack().getDataCollectionDsl();
    }
}

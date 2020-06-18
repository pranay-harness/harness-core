package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.TimeSeriesDataCollectionRecord;

import java.util.List;

public interface TimeSeriesService { boolean save(List<TimeSeriesDataCollectionRecord> dataRecords); }

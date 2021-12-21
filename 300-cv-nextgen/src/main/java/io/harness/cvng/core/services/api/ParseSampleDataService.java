package io.harness.cvng.core.services.api;

import io.harness.cvng.core.beans.TimeSeriesSampleDTO;
import io.harness.cvng.core.beans.params.ProjectParams;

import java.util.List;

public interface ParseSampleDataService {
  List<TimeSeriesSampleDTO> parseSampleData(ProjectParams projectParams, String jsonResponse, String groupName,
      String metricValueJsonPath, String timestampJsonPath, String timestampFormat);
}

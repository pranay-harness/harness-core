package software.wings.service.impl.stackdriver;

import com.google.api.services.monitoring.v3.Monitoring;

import lombok.Builder;
import lombok.Data;
import software.wings.service.impl.ThirdPartyApiCallLog;

import java.util.List;
import java.util.Optional;

@Data
@Builder
public class StackdriverDataFetchParameters {
  private String nameSpace;
  private String metric;
  private String dimensionValue;
  private String groupName;
  private String filter;
  private String projectId;
  private Monitoring monitoring;
  private Optional<List<String>> groupByFields;
  private Optional<String> perSeriesAligner;
  private Optional<String> crossSeriesReducer;
  private long startTime;
  private long endTime;
  private int dataCollectionMinute;
  private ThirdPartyApiCallLog apiCallLog;
  private boolean is247Task;
}

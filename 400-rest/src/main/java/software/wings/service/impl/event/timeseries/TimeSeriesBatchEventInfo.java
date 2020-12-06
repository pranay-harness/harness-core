package software.wings.service.impl.event.timeseries;

import io.harness.event.model.EventInfo;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TimeSeriesBatchEventInfo implements EventInfo {
  private String accountId;
  private long timestamp;
  private List<DataPoint> dataPointList;

  @Value
  @Builder
  public static class DataPoint {
    private Map<String, Object> data;
  }
}

package software.wings.service.impl.event.timeseries;

import io.harness.event.model.EventInfo;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class TimeSeriesEventInfo implements EventInfo {
  long timestamp;

  private Map<String, String> stringData;

  private Map<String, List<String>> listData;

  private Map<String, Integer> integerData;

  private Map<String, Long> longData;

  private Map<String, Float> floatData;

  private Map<String, Boolean> booleanData;

  private String accountId;
}

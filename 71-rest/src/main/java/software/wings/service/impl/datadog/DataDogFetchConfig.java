package software.wings.service.impl.datadog;

import lombok.Builder;
import lombok.Data;

/**
 * Created by Pranjal on 10/23/2018
 */
@Data
@Builder
public class DataDogFetchConfig {
  private String datadogServiceName;
  private String metrics;
  private String hostName;
  private long fromtime;
  private long toTime;
}

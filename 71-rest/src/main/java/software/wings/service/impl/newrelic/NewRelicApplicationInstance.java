package software.wings.service.impl.newrelic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Created by rsingh on 8/29/17.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewRelicApplicationInstance {
  private long id;
  private String host;
  private int port;
}

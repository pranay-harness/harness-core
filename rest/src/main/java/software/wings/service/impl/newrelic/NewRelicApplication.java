package software.wings.service.impl.newrelic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Created by rsingh on 8/28/17.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewRelicApplication {
  private String name;
  private long id;
}

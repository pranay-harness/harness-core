package software.wings.service.impl.dynatrace;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Created by rsingh on 8/28/17.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DynaTraceApplication {
  private String entityId;
  private String displayName;
}

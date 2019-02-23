package software.wings.helpers.ext.bamboo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

/**
 * Created by sgurubelli on 8/29/17.
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Status {
  boolean finished;
  String prettyQueuedTime;
}

package software.wings.service.impl.newrelic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Created by rsingh on 8/29/17.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class NewRelicApplicationInstancesResponse {
  private List<NewRelicApplicationInstance> application_instances;
}

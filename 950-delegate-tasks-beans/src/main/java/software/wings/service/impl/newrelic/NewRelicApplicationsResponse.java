package software.wings.service.impl.newrelic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Created by rsingh on 8/28/17.
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewRelicApplicationsResponse {
  private List<NewRelicApplication> applications;
}

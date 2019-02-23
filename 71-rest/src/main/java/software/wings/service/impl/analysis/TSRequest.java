package software.wings.service.impl.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Created by sriram_parthasarathy on 9/19/17.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TSRequest {
  private String stateExecutionId;
  private String workflowExecutionId;
  private Set<String> nodes;
  private int analysisMinute;
  private int analysisStartMinute;
}

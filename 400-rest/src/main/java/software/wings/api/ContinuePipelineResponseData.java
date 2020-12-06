package software.wings.api;

import io.harness.tasks.ResponseData;

import software.wings.sm.ExecutionInterrupt;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Created by anubhaw on 12/20/17.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ContinuePipelineResponseData implements ResponseData {
  private Map<String, String> workflowVariables;
  private ExecutionInterrupt interrupt;
}

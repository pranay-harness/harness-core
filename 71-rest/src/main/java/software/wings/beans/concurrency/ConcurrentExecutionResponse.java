package software.wings.beans.concurrency;

import io.harness.distribution.constraint.Consumer;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;
import org.codehaus.jackson.annotate.JsonTypeName;
import software.wings.beans.WorkflowExecution;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Value
@Builder
@JsonTypeName("concurrentExecutionResponse")
public class ConcurrentExecutionResponse {
  Consumer.State state;
  ConcurrencyStrategy.UnitType unitType;
  List<WorkflowExecution> executions;
  @Default Map<String, Object> infrastructureDetails = new HashMap<>();
}

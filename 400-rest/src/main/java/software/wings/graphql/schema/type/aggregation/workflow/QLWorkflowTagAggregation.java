package software.wings.graphql.schema.type.aggregation.workflow;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.TagAggregation;

import lombok.Builder;
import lombok.Value;

/**
 * @author rktummala on 09/05/19
 */
@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLWorkflowTagAggregation implements TagAggregation {
  private QLWorkflowTagType entityType;
  private String tagName;
}

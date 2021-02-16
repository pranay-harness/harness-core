package software.wings.graphql.schema.type.aggregation.trigger;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.tag.QLTagInput;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
public class QLTriggerTagFilter implements EntityFilter {
  private QLTriggerTagType entityType;
  private List<QLTagInput> tags;
}

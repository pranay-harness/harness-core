package software.wings.graphql.schema.type.aggregation.billing;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.EntityFilter;

import java.util.List;

@Value
@Builder
public class QLK8sLabelInput implements EntityFilter {
  private String name;
  private List<String> values;
}

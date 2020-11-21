package software.wings.graphql.schema.type.aggregation.billing;

import software.wings.graphql.schema.type.aggregation.EntityFilter;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLK8sLabelInput implements EntityFilter {
  private String name;
  private List<String> values;
}

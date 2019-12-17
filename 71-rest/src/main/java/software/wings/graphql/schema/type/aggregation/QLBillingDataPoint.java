package software.wings.graphql.schema.type.aggregation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QLBillingDataPoint {
  QLReference key;
  Number value;
  Number avg;
  Number max;
}

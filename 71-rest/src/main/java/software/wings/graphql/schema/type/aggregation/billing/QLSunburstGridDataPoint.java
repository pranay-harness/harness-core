package software.wings.graphql.schema.type.aggregation.billing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QLSunburstGridDataPoint {
  String name;
  String trend;
  String value;
  String info;
}
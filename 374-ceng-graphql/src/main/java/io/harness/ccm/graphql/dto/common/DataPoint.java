package io.harness.ccm.graphql.dto.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataPoint {
  Reference key;
  Number value;
}

package io.harness.ccm.graphql.dto.common;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Reference {
  String id;
  String name;
  String type;
}

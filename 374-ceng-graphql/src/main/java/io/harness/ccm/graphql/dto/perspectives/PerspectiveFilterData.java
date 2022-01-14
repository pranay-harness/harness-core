package io.harness.ccm.graphql.dto.perspectives;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PerspectiveFilterData {
  List<String> values;
}

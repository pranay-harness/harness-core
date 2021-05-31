package io.harness.ccm.graphql.dto.perspectives;

import io.harness.ccm.views.graphql.QLCEViewFieldIdentifierData;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PerspectiveFieldsData {
  List<QLCEViewFieldIdentifierData> fieldIdentifierData;
}

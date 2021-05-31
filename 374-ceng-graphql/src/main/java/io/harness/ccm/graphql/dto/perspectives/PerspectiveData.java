package io.harness.ccm.graphql.dto.perspectives;

import io.harness.ccm.views.graphql.QLCEView;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PerspectiveData {
  List<QLCEView> sampleViews;
  List<QLCEView> customerViews;
}

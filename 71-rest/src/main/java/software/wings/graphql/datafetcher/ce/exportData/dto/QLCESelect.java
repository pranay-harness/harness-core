package software.wings.graphql.datafetcher.ce.exportData.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLCESelect {
  private List<String> labels;
}

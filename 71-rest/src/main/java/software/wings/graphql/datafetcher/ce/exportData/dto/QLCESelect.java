package software.wings.graphql.datafetcher.ce.exportData.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class QLCESelect {
  List<String> labels;
}

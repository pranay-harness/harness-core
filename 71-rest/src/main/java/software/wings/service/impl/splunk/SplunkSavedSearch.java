package software.wings.service.impl.splunk;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SplunkSavedSearch {
  String title;
  String searchQuery;
}

package software.wings.search.framework;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class AdvancedSearchQuery {
  private String searchQuery;
  private int numResults;
  private int offset;
  private List<String> entities;
}

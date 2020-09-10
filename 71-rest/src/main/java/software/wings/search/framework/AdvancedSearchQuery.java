package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@OwnedBy(PL)
@Value
@Builder
public class AdvancedSearchQuery {
  private String searchQuery;
  private int numResults;
  private int offset;
  private List<String> entities;
}

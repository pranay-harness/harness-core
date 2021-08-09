package software.wings.beans;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ResourceLookupFilter {
  private List<String> appIds;
  private List<String> resourceTypes;
  private HarnessTagFilter harnessTagFilter;
}

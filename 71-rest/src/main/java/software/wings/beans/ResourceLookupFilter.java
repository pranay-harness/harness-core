package software.wings.beans;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ResourceLookupFilter {
  private List<String> appIds;
  private List<String> resourceTypes;
  private HarnessTagFilter harnessTagFilter;
}

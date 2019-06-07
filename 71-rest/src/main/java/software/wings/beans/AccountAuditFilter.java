package software.wings.beans;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class AccountAuditFilter {
  private List<String> resourceTypes;
  private List<String> resourceIds;
}

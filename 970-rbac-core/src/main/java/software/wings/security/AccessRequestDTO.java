package software.wings.security;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.PL)
public class AccessRequestDTO {
  String accountId;
  String harnessUserGroupId;
  Set<String> emailIds;
  private long accessStartAt;
  private long accessEndAt;
  private Integer hours;
}

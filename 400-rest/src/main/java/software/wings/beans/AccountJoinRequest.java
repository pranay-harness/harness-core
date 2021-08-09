package software.wings.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AccountJoinRequest {
  private String name;
  private String email;
  private String companyName;
  private String note;
  private String accountAdminEmail;
}

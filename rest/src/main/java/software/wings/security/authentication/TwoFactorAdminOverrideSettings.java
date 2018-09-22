package software.wings.security.authentication;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TwoFactorAdminOverrideSettings {
  private boolean adminOverrideTwoFactorEnabled;
}

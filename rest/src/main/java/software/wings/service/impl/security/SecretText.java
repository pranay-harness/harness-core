package software.wings.service.impl.security;

import lombok.Builder;
import lombok.Data;
import software.wings.settings.UsageRestrictions;

/**
 * Created by rsingh on 11/15/17.
 */
@Data
@Builder
public class SecretText {
  private String name;
  private String value;
  private UsageRestrictions usageRestrictions;
}

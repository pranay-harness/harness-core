package software.wings.service.impl.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import software.wings.settings.UsageRestrictions;

/**
 * Created by rsingh on 11/15/17.
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecretText {
  private String name;
  private String value;
  private String path;
  private UsageRestrictions usageRestrictions;
  private String kmsId;
}

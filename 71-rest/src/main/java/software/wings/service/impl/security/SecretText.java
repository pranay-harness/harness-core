package software.wings.service.impl.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.security.encryption.SecretVariable;
import lombok.Builder;
import lombok.Data;
import software.wings.settings.UsageRestrictions;

import java.util.Set;

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
  private Set<SecretVariable> variables;
  private UsageRestrictions usageRestrictions;
  private String kmsId;
}

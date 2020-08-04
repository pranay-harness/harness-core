package software.wings.service.impl.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.security.encryption.EncryptedDataParams;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import software.wings.settings.UsageRestrictions;

import java.util.Map;
import java.util.Set;

/**
 * Created by rsingh on 11/15/17.
 */
@Data
@Builder
@FieldNameConstants(innerTypeName = "SecretTextKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecretText {
  private String name;
  private String value;
  private String path;
  private Set<EncryptedDataParams> parameters;
  private UsageRestrictions usageRestrictions;
  private String kmsId;
  private Map<String, String> runtimeParameters;
  private boolean scopedToAccount;
  private boolean hideFromListing;
}

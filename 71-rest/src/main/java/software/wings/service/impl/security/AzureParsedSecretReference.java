package software.wings.service.impl.security;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.base.Preconditions;

import io.harness.annotations.dev.OwnedBy;
import lombok.Value;

@OwnedBy(PL)
@Value
public class AzureParsedSecretReference {
  public static final char SECRET_NAME_AND_VERSION_SEPARATOR = '/';

  String secretName;
  String secretVersion;

  public AzureParsedSecretReference(String secretPath) {
    Preconditions.checkState(isNotBlank(secretPath), "'secretPath' is blank");

    int separatorIndex = secretPath.indexOf(SECRET_NAME_AND_VERSION_SEPARATOR);
    if (separatorIndex != -1) {
      secretName = secretPath.substring(0, separatorIndex);
      secretVersion = secretPath.substring(separatorIndex + 1);
    } else {
      secretName = secretPath;
      secretVersion = "";
    }
  }
}

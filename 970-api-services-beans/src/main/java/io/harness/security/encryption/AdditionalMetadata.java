package io.harness.security.encryption;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode
public class AdditionalMetadata {
  public static final String SECRET_KEY_UUID_KEY = "SecretKey";
  public static final String AWS_ENCRYPTED_SECRET = "AwsEncryptedSecret";

  private Map<String, Object> values;

  public Map<String, Object> addValues(Map<String, Object> newValues) {
    newValues.forEach((k, v) -> { values.put(k, v); });
    return values;
  }

  public String getSecretKeyUuid() {
    return (String) values.get(SECRET_KEY_UUID_KEY);
  }

  public byte[] getAwsEncryptedSecret() {
    return (byte[]) values.get(AWS_ENCRYPTED_SECRET);
  }
}

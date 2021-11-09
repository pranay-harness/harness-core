package io.harness.security.encryption;

import java.util.Map;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
@EqualsAndHashCode
public class AdditionalMetadata {
  public static final String SECRET_KEY_KEY = "SecretKey";
  public static final String AWS_ENCRYPTED_SECRET = "AwsEncryptedSecret";

  @Singular private Map<String, Object> values;

  public Map<String, Object> addValues(Map<String, Object> newValues) {
    newValues.forEach(values::put);
    return values;
  }
}

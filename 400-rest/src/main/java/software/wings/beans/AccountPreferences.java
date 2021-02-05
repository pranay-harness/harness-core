package software.wings.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@FieldNameConstants(innerTypeName = "AccountPreferencesKeys")
public class AccountPreferences {
  Integer delegateSecretsCacheTTLInHours = 1;
}

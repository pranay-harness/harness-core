package software.wings.service.impl.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by rsingh on 11/3/17.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class VaultSecretValue {
  private String value;
}

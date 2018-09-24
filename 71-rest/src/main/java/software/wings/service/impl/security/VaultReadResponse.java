package software.wings.service.impl.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

/**
 * Created by rsingh on 11/3/17.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class VaultReadResponse {
  private VaultSecretValue data;
}

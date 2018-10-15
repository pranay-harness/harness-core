package software.wings.service.impl.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

/**
 * This is a response DTO class to accommodate the the response format from Vault services backed by the v2
 * secret engine such as version 0.11.
 *
 * @author mark.lu on 10/11/18
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class VaultReadResponseV2 {
  private VaultSecretValueV2 data;
}

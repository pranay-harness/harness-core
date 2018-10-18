package software.wings.service.impl.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * This is a request DTO class to accommodate the the request data format for Vault services backed by the v2
 * secret engine such as version 0.11.
 *
 * @author mark.lu on 10/11/18
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class VaultSecretValueV2 {
  private Map<String, String> data;
}

package software.wings.service.impl.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

/**
 * This DTO class is for reading Vault secret metadata, especially the 'versions' information for constructing the
 * secret change log to be presented in the UI.
 *
 * More details on Vault versions metadata can be found at:
 *  https://learn.hashicorp.com/vault/secrets-management/sm-versioned-kv
 *
 * @author marklu on 2018-12-07
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class VaultMetadataReadResponse {
  private VaultSecretMetadata data;
}

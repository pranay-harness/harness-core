package software.wings.helpers.ext.vault;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import software.wings.service.impl.security.vault.SysMountsResponse;
import software.wings.service.impl.security.vault.VaultAppRoleLoginRequest;
import software.wings.service.impl.security.vault.VaultAppRoleLoginResponse;

/**
 * We need a way to automatically determine the Vault server's secret engine version. The secret engine version could be
 * parsed from the JSON response's "/secret/options/version" field.
 *    curl --header "X-Vault-Token: $VAULT_TOKEN" http://HOST:PORT/v1/sys/mounts
 *
 * The secret engine version information is needed for Vault Service to decide which REST API Client is used to talk to
 * the Vault server,
 */
public interface VaultSysAuthRestClient {
  String BASE_VAULT_URL = "v1/sys/mounts";
  String APPROLE_LOGIN_URL = "v1/auth/approle/login";

  /**
   * The JSON response will be returned as a String. The caller of this API need to '/secret/options/version' field in
   * the JSON document to extract the secret engine version.
   */
  @GET(BASE_VAULT_URL) Call<SysMountsResponse> getAllMounts(@Header("X-Vault-Token") String header);

  @POST(APPROLE_LOGIN_URL) Call<VaultAppRoleLoginResponse> appRoleLogin(@Body VaultAppRoleLoginRequest request);
}

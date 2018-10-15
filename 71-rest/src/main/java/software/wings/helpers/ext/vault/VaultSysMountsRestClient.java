package software.wings.helpers.ext.vault;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

/**
 * We need a way to automatically determine the Vault server's secret engine version. The secret engine version could be
 * parsed from the JSON response's "/secret/options/version" field.
 *    curl --header "X-Vault-Token: $VAULT_TOKEN" http://localhost:8200/v1/sys/mounts
 *
 * The secret engine version information is needed for Vault Service to decide which REST API Client is used to talk to
 * the Vault server,
 */
public interface VaultSysMountsRestClient {
  String BASE_VAULT_URL = "v1/sys/mounts";

  /**
   * The JSON response will be returned as a String. The caller of this API need to '/secret/options/version' field in
   * the JSON document to extract the secret engine version.
   */
  @GET(BASE_VAULT_URL) Call<ResponseBody> getAll(@Header("X-Vault-Token") String header);
}

package software.wings.helpers.ext.vault;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import software.wings.service.impl.security.VaultReadResponseV2;
import software.wings.service.impl.security.VaultSecretValueV2;
import software.wings.settings.SettingValue.SettingVariableTypes;

/**
 * It appears that the latest Vault (v0.11) was switched to use the v2 key/value secret engine by default. The endpoint
 * for KV v2 is /secret/data/$PATH$ Therefore to write secretes at /secret/customer/acme, the API endpoints becomes
 * /secret/data/customer/acme.
 *
 * Please refer to the following Hashicorp documentation for details on secret engine v2.
 * https://www.vaultproject.io/guides/secret-mgmt/versioned-kv.html
 *
 * To handle the newer version of Vault, we will need a new REST client to accommodate the differences compared with v1
 * Vault.
 *
 * @author mark.lu on 10/11/18
 */
public interface VaultRestClientV2 {
  String BASE_VAULT_URL = "v1/secret/data/harness/";

  @POST(BASE_VAULT_URL + "{variableType}/{keyName}")
  Call<Void> writeSecret(@Header("X-Vault-Token") String header, @Path("keyName") String keyName,
      @Path("variableType") SettingVariableTypes settingType, @Body VaultSecretValueV2 value);

  @DELETE(BASE_VAULT_URL + "{variableType}/{path}")
  Call<Void> deleteSecret(@Header("X-Vault-Token") String header, @Path("path") String path);

  @GET(BASE_VAULT_URL + "{path}")
  Call<VaultReadResponseV2> readSecret(@Header("X-Vault-Token") String header, @Path("path") String keyName);

  @POST("v1/auth/token/renew-self") Call<Object> renewToken(@Header("X-Vault-Token") String header);
}

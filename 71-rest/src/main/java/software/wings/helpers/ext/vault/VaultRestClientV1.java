package software.wings.helpers.ext.vault;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import software.wings.service.impl.security.VaultReadResponse;
import software.wings.service.impl.security.VaultSecretValue;
import software.wings.settings.SettingValue.SettingVariableTypes;

/**
 * Created by rsingh on 11/3/17.
 */
public interface VaultRestClientV1 {
  String BASE_VAULT_URL = "v1/secret/harness/";

  @POST(BASE_VAULT_URL + "{variableType}/{keyName}")
  Call<Void> writeSecret(@Header("X-Vault-Token") String header, @Path("keyName") String keyName,
      @Path("variableType") SettingVariableTypes settingType, @Body VaultSecretValue value);

  @DELETE(BASE_VAULT_URL + "{variableType}/{path}")
  Call<Void> deleteSecret(@Header("X-Vault-Token") String header, @Path("path") String path);

  @GET(BASE_VAULT_URL + "{path}")
  Call<VaultReadResponse> readSecret(@Header("X-Vault-Token") String header, @Path("path") String keyName);

  @POST("v1/auth/token/renew-self") Call<Object> renewToken(@Header("X-Vault-Token") String header);
}

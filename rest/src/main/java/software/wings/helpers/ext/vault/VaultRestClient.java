package software.wings.helpers.ext.vault;

import retrofit2.Call;
import retrofit2.http.Body;
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
public interface VaultRestClient {
  String BASE_VAULT_URL = "v1/secret/harness/";

  @POST(BASE_VAULT_URL + "{accountId}/{variableType}/{keyName}/{uuid}")
  Call<Void> writeSecret(@Header("X-Vault-Token") String header, @Path("keyName") String keyName,
      @Path("accountId") String accountId, @Path("variableType") SettingVariableTypes settingType,
      @Path("uuid") String uuid, @Body VaultSecretValue value);

  @GET(BASE_VAULT_URL + "{keyName}")
  Call<VaultReadResponse> readSecret(@Header("X-Vault-Token") String header, @Path("keyName") String keyName);
}

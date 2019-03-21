package io.harness.RestUtils;

import io.harness.beans.PageResponse;
import io.harness.framework.Setup;
import io.harness.rest.RestResponse;
import io.restassured.mapper.ObjectMapperType;
import software.wings.beans.VaultConfig;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretText;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.List;
import javax.ws.rs.core.GenericType;

public class SecretsRestUtils {
  public List<VaultConfig> getListConfigs(String accountId, String bearerToken) {
    RestResponse<List<VaultConfig>> secretsResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", accountId)
            .get("/secrets/list-values")
            .as(new GenericType<RestResponse<List<VaultConfig>>>() {}.getType());
    return secretsResponse.getResource();
  }

  public String addSecret(String accountId, String bearerToken, SecretText secretText) {
    //    Gson gson = new Gson();
    //    JsonElement json = gson.fromJson(gson.toJson(secretText), JsonElement.class);
    //    JsonObject jObj = json.getAsJsonObject();
    RestResponse<String> secretsResponse = Setup.portal()
                                               .auth()
                                               .oauth2(bearerToken)
                                               .queryParam("accountId", accountId)
                                               .body(secretText, ObjectMapperType.GSON)
                                               .post("/secrets/add-secret")
                                               .as(new GenericType<RestResponse<String>>() {}.getType());
    return secretsResponse.getResource();
  }

  public boolean updateSecret(String accountId, String bearerToken, String uuid, SecretText secretText) {
    RestResponse<Boolean> secretsResponse = Setup.portal()
                                                .auth()
                                                .oauth2(bearerToken)
                                                .queryParam("accountId", accountId)
                                                .queryParam("uuid", uuid)
                                                .body(secretText, ObjectMapperType.GSON)
                                                .post("/secrets/update-secret")
                                                .as(new GenericType<RestResponse<Boolean>>() {}.getType());
    return secretsResponse.getResource();
  }

  public boolean deleteSecret(String accountId, String bearerToken, String uuid) {
    RestResponse<Boolean> secretsResponse = Setup.portal()
                                                .auth()
                                                .oauth2(bearerToken)
                                                .queryParam("accountId", accountId)
                                                .queryParam("uuid", uuid)
                                                .delete("/secrets/delete-secret")
                                                .as(new GenericType<RestResponse<Boolean>>() {}.getType());
    return secretsResponse.getResource();
  }

  public List<EncryptedData> listSecrets(String accountId, String bearerToken) {
    RestResponse<PageResponse<EncryptedData>> encryptedDataList =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", accountId)
            .queryParam("type", SettingVariableTypes.SECRET_TEXT)
            .get("/secrets/list-secrets-page")
            .as(new GenericType<RestResponse<PageResponse<EncryptedData>>>() {}.getType());
    return encryptedDataList.getResource().getResponse();
  }
}

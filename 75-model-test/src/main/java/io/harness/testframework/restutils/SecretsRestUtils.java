package io.harness.testframework.restutils;

import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;

import com.google.gson.JsonElement;

import io.harness.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.utils.SecretsUtils;
import io.restassured.mapper.ObjectMapperType;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.VaultConfig;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretText;
import software.wings.settings.SettingVariableTypes;

import java.io.File;
import java.util.List;
import javax.ws.rs.core.GenericType;

@Slf4j
public class SecretsRestUtils {
  public static List<VaultConfig> getListConfigs(String accountId, String bearerToken) {
    RestResponse<List<VaultConfig>> secretsResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", accountId)
            .get("/secrets/list-values")
            .as(new GenericType<RestResponse<List<VaultConfig>>>() {}.getType());
    return secretsResponse.getResource();
  }

  public static String addSecret(String accountId, String bearerToken, SecretText secretText) {
    RestResponse<String> secretsResponse = Setup.portal()
                                               .auth()
                                               .oauth2(bearerToken)
                                               .queryParam("accountId", accountId)
                                               .body(secretText, ObjectMapperType.GSON)
                                               .post("/secrets/add-secret")
                                               .as(new GenericType<RestResponse<String>>() {}.getType());
    return secretsResponse.getResource();
  }

  public static boolean updateSecret(String accountId, String bearerToken, String uuid, SecretText secretText) {
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

  public static boolean deleteSecret(String accountId, String bearerToken, String uuid) {
    RestResponse<Boolean> secretsResponse = Setup.portal()
                                                .auth()
                                                .oauth2(bearerToken)
                                                .queryParam("accountId", accountId)
                                                .queryParam("uuid", uuid)
                                                .delete("/secrets/delete-secret")
                                                .as(new GenericType<RestResponse<Boolean>>() {}.getType());
    return secretsResponse.getResource();
  }

  public static List<EncryptedData> listSecrets(String accountId, String bearerToken) {
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

  public static String addSecretWithUsageRestrictions(String accountId, String bearerToken, SecretText secretText) {
    RestResponse<String> secretsResponse = null;
    try {
      logger.info("Entering add Secret with restrictions");
      JsonElement jsonElement = SecretsUtils.getUsageRestDataAsJson(secretText);
      logger.info(jsonElement.toString());
      secretsResponse = Setup.portal()
                            .auth()
                            .oauth2(bearerToken)
                            .queryParam("accountId", accountId)
                            .body(jsonElement.toString())
                            .post("/secrets/add-secret")
                            .as(new GenericType<RestResponse<String>>() {}.getType());
      logger.info(secretsResponse.toString());
      logger.info("Secret Id : " + secretsResponse.getResource());
    } catch (Exception e) {
      logger.error("Exception thrown : ", e);
    }

    if (secretsResponse != null) {
      return secretsResponse.getResource();
    }
    return null;
  }

  public static boolean updateSecretWithUsageRestriction(
      String accountId, String bearerToken, String uuid, SecretText secretText) {
    JsonElement jsonElement = SecretsUtils.getUsageRestDataAsJson(secretText);
    RestResponse<Boolean> secretsResponse = Setup.portal()
                                                .auth()
                                                .oauth2(bearerToken)
                                                .queryParam("accountId", accountId)
                                                .queryParam("uuid", uuid)
                                                .body(jsonElement.toString())
                                                .post("/secrets/update-secret")
                                                .as(new GenericType<RestResponse<Boolean>>() {}.getType());
    return secretsResponse.getResource();
  }

  public static String addSecretFile(String accountId, String bearerToken, String name, String fileName) {
    File file = new File(fileName);
    RestResponse<String> secretsResponse = Setup.portal()
                                               .auth()
                                               .oauth2(bearerToken)
                                               .contentType(MULTIPART_FORM_DATA)
                                               .queryParam("accountId", accountId)
                                               .formParam("name", name)
                                               .multiPart("file", file, "text/html")
                                               .formParam("usageRestrictions", "{\"appEnvRestrictions\":[]}")
                                               .post("/secrets/add-file")
                                               .as(new GenericType<RestResponse<String>>() {}.getType());
    return secretsResponse.getResource();
  }

  public static boolean updateSecretFile(
      String accountId, String bearerToken, String name, String fileName, String uuid) {
    File file = new File(fileName);
    RestResponse<Boolean> secretsResponse = Setup.portal()
                                                .auth()
                                                .oauth2(bearerToken)
                                                .contentType(MULTIPART_FORM_DATA)
                                                .queryParam("accountId", accountId)
                                                .formParam("name", name)
                                                .multiPart("file", file, "text/html")
                                                .formParam("usageRestrictions", "{\"appEnvRestrictions\":[]}")
                                                .formParam("uuid", uuid)
                                                .post("/secrets/update-file")
                                                .as(new GenericType<RestResponse<Boolean>>() {}.getType());
    return secretsResponse.getResource();
  }

  public static boolean deleteSecretFile(String accountId, String bearerToken, String uuid) {
    RestResponse<Boolean> secretsResponse = Setup.portal()
                                                .auth()
                                                .oauth2(bearerToken)
                                                .queryParam("accountId", accountId)
                                                .queryParam("uuid", uuid)
                                                .delete("/secrets/delete-file")
                                                .as(new GenericType<RestResponse<Boolean>>() {}.getType());
    return secretsResponse.getResource();
  }

  public static List<EncryptedData> listSecretsFile(String accountId, String bearerToken) {
    RestResponse<PageResponse<EncryptedData>> encryptedDataList =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", accountId)
            .queryParam("type", SettingVariableTypes.CONFIG_FILE)
            .get("/secrets/list-secrets-page")
            .as(new GenericType<RestResponse<PageResponse<EncryptedData>>>() {}.getType());
    return encryptedDataList.getResource().getResponse();
  }
}

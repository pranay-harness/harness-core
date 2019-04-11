package io.harness.RestUtils;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Singleton;

import io.harness.framework.Setup;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.path.json.JsonPath;
import org.junit.Assert;
import software.wings.beans.SettingAttribute;

import java.util.ArrayList;
import java.util.HashMap;

@Singleton
public class SettingsUtil {
  private static String ACCOUNT_ID = "accountId";
  private static String SETTINGS_ENDPOINT = "/settings";

  public static JsonPath create(String bearerToken, String accountId, SettingAttribute setAttr) {
    JsonPath response = Setup.portal()
                            .auth()
                            .oauth2(bearerToken)
                            .queryParam(ACCOUNT_ID, accountId)
                            .body(setAttr, ObjectMapperType.GSON)
                            .contentType(ContentType.JSON)
                            .post(SETTINGS_ENDPOINT)
                            .jsonPath();
    return response;
  }

  public static JsonPath listCloudproviderConnector(String bearerToken, String accountId, String category) {
    JsonPath response = Setup.portal()
                            .auth()
                            .oauth2(bearerToken)
                            .queryParam(ACCOUNT_ID, accountId)
                            .queryParam("search[0][field]", "category")
                            .queryParam("search[0][op]", "EQ")
                            .queryParam("search[0][value]", category)
                            .contentType(ContentType.JSON)
                            .get(SETTINGS_ENDPOINT)
                            .jsonPath();
    return response;
  }

  public static JsonPath validateConnectivity(String bearerToken, String accountId, SettingAttribute setAttr) {
    JsonPath response = Setup.portal()
                            .auth()
                            .oauth2(bearerToken)
                            .queryParam(ACCOUNT_ID, accountId)
                            .body(setAttr, ObjectMapperType.GSON)
                            .contentType(ContentType.JSON)
                            .post("/validate-connectivity")
                            .jsonPath();
    return response;
  }

  public static JsonPath validate(String bearerToken, String accountId, SettingAttribute setAttr) {
    JsonPath response = Setup.portal()
                            .auth()
                            .oauth2(bearerToken)
                            .queryParam(ACCOUNT_ID, accountId)
                            .body(setAttr, ObjectMapperType.GSON)
                            .contentType(ContentType.JSON)
                            .post("/validate")
                            .jsonPath();
    return response;
  }

  public static JsonPath saveGCP(String bearerToken, String accntId) {
    JsonPath response = Setup.portal()
                            .auth()
                            .oauth2(bearerToken)
                            .queryParam(ACCOUNT_ID, accntId)
                            .body("", ObjectMapperType.GSON)
                            .contentType(ContentType.JSON)
                            .post("/upload")
                            .jsonPath();
    return response;
  }

  public static void delete(String bearerToken, String accountId, String settingAttrId) {
    Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam(ACCOUNT_ID, accountId)
        .queryParam("isPluginSetting", true)
        .delete(SETTINGS_ENDPOINT + "/" + settingAttrId);
  }

  /*
    Use this to check how many total number of cloudprovider/connectors are present. Use this in case of,
    1. Should be handy in verifying the new connector created by count
    2. Should be handy in verifying the existing connector is deleted
   */
  public static int getCloudproviderConnectorCount(String bearerToken, String accountId, String category) {
    // Get all the cloudprovider/connectors
    JsonPath connectors = SettingsUtil.listCloudproviderConnector(bearerToken, accountId, category);
    assertThat(connectors).isNotNull();

    ArrayList<HashMap<String, String>> hashMaps =
        (ArrayList<HashMap<String, String>>) connectors.getMap("resource").get("response");
    for (HashMap<String, String> data : hashMaps) {
      if (!data.get("category").equals(category)) {
        Assert.fail("ERROR: Found an entry which is not of type '" + category + "'");
      }
    }
    return hashMaps.size();
  }

  /*
    Use this function when you want to check if the cloudprovider/connector with specific name exist.
    Use this in case of,
    1. to check and create new connector (create only when it's not found)
    2. to verify new connector created
    3. to delete a connector and check if its deleted
   */
  public static boolean checkCloudproviderConnectorExist(
      String bearerToken, String accountId, String category, String name) {
    boolean connFound = false;
    // Get all the cloudprovider/connectors
    JsonPath connectors = SettingsUtil.listCloudproviderConnector(bearerToken, accountId, category);
    assertThat(connectors).isNotNull();

    ArrayList<HashMap<String, String>> hashMaps =
        (ArrayList<HashMap<String, String>>) connectors.getMap("resource").get("response");

    for (HashMap<String, String> data : hashMaps) {
      if (data.get("name").equals(name)) {
        connFound = true;
        break;
      }
    }
    return connFound;
  }
}
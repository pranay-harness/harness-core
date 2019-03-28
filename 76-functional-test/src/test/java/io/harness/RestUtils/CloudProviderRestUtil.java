package io.harness.RestUtils;

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
public class CloudProviderRestUtil {
  public static JsonPath create(String bearerToken, String accountId, SettingAttribute setAttr) {
    JsonPath response = Setup.portal()
                            .auth()
                            .oauth2(bearerToken)
                            .queryParam("accountId", accountId)
                            .body(setAttr, ObjectMapperType.GSON)
                            .contentType(ContentType.JSON)
                            .post("/settings")
                            .jsonPath();
    return response;
  }

  public static JsonPath list(String bearerToken, String accountId) {
    JsonPath response = Setup.portal()
                            .auth()
                            .oauth2(bearerToken)
                            .queryParam("accountId", accountId)
                            .queryParam("search[0][field]", "category")
                            .queryParam("search[0][op]", "EQ")
                            .queryParam("search[0][value]", "CLOUD_PROVIDER")
                            .contentType(ContentType.JSON)
                            .get("/settings")
                            .jsonPath();
    return response;
  }

  public static JsonPath validateConnectivity(String bearerToken, String accountId, SettingAttribute setAttr) {
    JsonPath response = Setup.portal()
                            .auth()
                            .oauth2(bearerToken)
                            .queryParam("accountId", accountId)
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
                            .queryParam("accountId", accountId)
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
                            .queryParam("accountId", accntId)
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
        .queryParam("accountId", accountId)
        .queryParam("isPluginSetting", true)
        .delete("/settings/" + settingAttrId);
  }

  /*
    This function verifies if the Setting Attributes are of type 'Cloud Providers' and It also Returns the number of
    CloudProviders found.
   */
  public static int verifyCloudProviders(String bearerToken, String accountId, JsonPath cloudProviders) {
    ArrayList<HashMap<String, String>> hashMaps =
        (ArrayList<HashMap<String, String>>) cloudProviders.getMap("resource").get("response");
    for (HashMap<String, String> data : hashMaps) {
      if (!data.get("category").equals("CLOUD_PROVIDER")) {
        Assert.fail("ERROR: Found an Entry which is not of type 'CLOUD_PROVIDER'");
      } else {
        // System.out.println("CP :" + data.get("category"));
      }
    }
    return hashMaps.size();
  }
}
package io.harness.helper;

import io.harness.framework.Setup;
import io.harness.functional.AbstractFunctionalTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GlobalSettingsDataStorage extends AbstractFunctionalTest {
  public static Map<String, String> globalDataMap = new HashMap<>();
  public static Map<String, String> globalSecrets = new HashMap<>();

  public Map<String, String> getAvailableGlobalDataMap() {
    Map<String, String> temp = new HashMap<>();
    JsonPath jsonPath =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", getAccount().getUuid())
            .queryParam(
                "search[0][field]=category&search[0][op]=IN&search[0][value]=CLOUD_PROVIDER&search[0][value]=CONNECTOR")
            .contentType(ContentType.JSON)
            .get("/settings")
            .jsonPath();
    ArrayList<HashMap<String, String>> hashMaps =
        (ArrayList<HashMap<String, String>>) jsonPath.getMap("resource").get("response");
    for (HashMap<String, String> data : hashMaps) {
      temp.putIfAbsent(data.get("name"), data.get("uuid"));
    }
    if (temp.size() > 0) {
      globalDataMap = temp;
    }

    return globalDataMap;
  }

  public Map<String, String> getAvailableSecrets() {
    Map<String, String> temp = new HashMap<>();
    JsonPath jsonPath = Setup.portal()
                            .auth()
                            .oauth2(bearerToken)
                            .queryParam("accountId", getAccount().getUuid())
                            .contentType(ContentType.JSON)
                            .get("/secrets/list-values")
                            .getBody()
                            .jsonPath();
    ArrayList<HashMap<String, String>> hashMaps =
        (ArrayList<HashMap<String, String>>) jsonPath.getMap("resource").get("response");
    for (HashMap<String, String> data : hashMaps) {
      temp.putIfAbsent(data.get("name"), data.get("uuid"));
    }
    if (temp.size() > 0) {
      globalSecrets = temp;
    }
    return globalSecrets;
  }
}
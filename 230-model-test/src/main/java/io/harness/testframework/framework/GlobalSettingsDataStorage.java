/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.framework;

import software.wings.beans.Account;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GlobalSettingsDataStorage {
  private static Map<String, String> globalDataMap = new HashMap<>();
  private static Map<String, String> globalSecrets = new HashMap<>();

  public static Map<String, String> getAvailableGlobalDataMap(String bearerToken, Account account) {
    Map<String, String> temp = new HashMap<>();
    JsonPath jsonPath =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", account.getUuid())
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

  public static Map<String, String> getAvailableSecrets(String bearerToken, Account account) {
    Map<String, String> temp = new HashMap<>();
    JsonPath jsonPath = Setup.portal()
                            .auth()
                            .oauth2(bearerToken)
                            .queryParam("accountId", account.getUuid())
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

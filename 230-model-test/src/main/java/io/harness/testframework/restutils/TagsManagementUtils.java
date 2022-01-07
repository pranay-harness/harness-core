/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.restutils;

import io.harness.testframework.framework.Setup;

import software.wings.beans.HarnessTag;
import software.wings.beans.HarnessTagLink;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;

public class TagsManagementUtils {
  private static String ACCOUNT_ID = "accountId";
  private static String KEY = "key";
  private static String TAG_ENDPOINT = "/tags";
  private static String ATTACH_TAG_ENDPOINT = "/tags/attach";
  private static String TAG_LINKS_ENDPOINT = "/tags/links";

  private static String APP_ID = "appId";

  public static JsonPath listTags(String bearerToken, String accountId) {
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam(ACCOUNT_ID, accountId)
        .queryParam("sort[0][field]", "key")
        .queryParam("sort[0][direction]", "ASC")
        .queryParam("offset", 0)
        .queryParam("limit", 40)
        .queryParam("includeInUseValues", true)
        .contentType(ContentType.JSON)
        .get(TAG_ENDPOINT)
        .jsonPath();
  }

  public static JsonPath createTag(String bearerToken, String accountId, HarnessTag tag) {
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam(ACCOUNT_ID, accountId)
        .contentType(ContentType.JSON)
        .body(tag)
        .post(TAG_ENDPOINT)
        .jsonPath();
  }

  public static JsonPath editTag(String bearerToken, String accountId, String key, HarnessTag tag) {
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam(KEY, key)
        .queryParam(ACCOUNT_ID, accountId)
        .contentType(ContentType.JSON)
        .body(tag)
        .put(TAG_ENDPOINT)
        .jsonPath();
  }

  public static JsonPath attachTag(String bearerToken, String accountId, String appId, HarnessTagLink tagLink) {
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam(ACCOUNT_ID, accountId)
        .queryParam(APP_ID, appId)
        .contentType(ContentType.JSON)
        .body(tagLink)
        .post(ATTACH_TAG_ENDPOINT)
        .jsonPath();
  }

  public static JsonPath deleteTag(String bearerToken, String accountId, String tagKey) {
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam(KEY, tagKey)
        .queryParam(ACCOUNT_ID, accountId)
        .contentType(ContentType.JSON)
        .delete(TAG_ENDPOINT)
        .jsonPath();
  }

  public static JsonPath getTagUsageDetails(String bearerToken, String accountId, String tagKey) {
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam(ACCOUNT_ID, accountId)
        .queryParam("sort[0][field]", "value")
        .queryParam("sort[0][direction]", "ASC")
        .queryParam("offset", 0)
        .queryParam("limit", 10000)
        .queryParam("search[0][field]", "key")
        .queryParam("search[0][op]", "EQ")
        .queryParam("search[0][value]", tagKey)
        .contentType(ContentType.JSON)
        .get(TAG_LINKS_ENDPOINT)
        .jsonPath();
  }

  public static JsonPath getTag(String bearerToken, String accountId, String tagKey) {
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam(KEY, tagKey)
        .queryParam(ACCOUNT_ID, accountId)
        .contentType(ContentType.JSON)
        .get(TAG_ENDPOINT)
        .jsonPath();
  }
}

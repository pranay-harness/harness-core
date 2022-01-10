/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.restutils;

import io.harness.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;

import software.wings.beans.Account;
import software.wings.beans.security.UserGroup;
import software.wings.beans.sso.LdapGroupResponse;
import software.wings.security.PermissionAttribute.PermissionType;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.restassured.mapper.ObjectMapperType;
import java.util.Iterator;
import java.util.List;
import javax.ws.rs.core.GenericType;

public class UserGroupRestUtils {
  public static List<UserGroup> getUserGroups(Account account, String bearerToken) {
    RestResponse<PageResponse<UserGroup>> userGroups =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", account.getUuid())
            .get("/userGroups")
            .as(new GenericType<RestResponse<PageResponse<UserGroup>>>() {}.getType());
    return userGroups.getResource();
  }

  public static UserGroup getUserGroup(Account account, String bearerToken, String userGroupId) {
    RestResponse<UserGroup> userGroup = Setup.portal()
                                            .auth()
                                            .oauth2(bearerToken)
                                            .queryParam("accountId", account.getUuid())
                                            .get("/userGroups/" + userGroupId)
                                            .as(new GenericType<RestResponse<PageResponse<UserGroup>>>() {}.getType());
    return userGroup.getResource();
  }

  public static UserGroup createUserGroup(Account account, String bearerToken, JsonObject jsonObject) {
    RestResponse<UserGroup> userGroups = Setup.portal()
                                             .auth()
                                             .oauth2(bearerToken)
                                             .queryParam("accountId", account.getUuid())
                                             .body(jsonObject.toString())
                                             .post("/userGroups")
                                             .as(new GenericType<RestResponse<UserGroup>>() {}.getType());
    return userGroups.getResource();
  }

  public static Integer updateMembers(Account account, String bearerToken, UserGroup userGroup) {
    JsonArray jsonArray = new JsonArray();
    JsonObject jObj;
    JsonObject masterObj = new JsonObject();
    for (int i = 0; i < userGroup.getMemberIds().size(); i++) {
      jObj = new JsonObject();
      jObj.addProperty("uuid", userGroup.getMemberIds().get(i));
      jsonArray.add(jObj);
    }

    masterObj.add("members", jsonArray);

    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", account.getUuid())
        .body(masterObj, ObjectMapperType.GSON)
        .put("/userGroups/" + userGroup.getUuid() + "/members")
        .statusCode();
  }

  public static Integer updateAccountPermissions(Account account, String bearerToken, UserGroup userGroup) {
    JsonArray jsonArray = new JsonArray();
    JsonObject jObj = new JsonObject();
    JsonObject masterObj = new JsonObject();
    Iterator<PermissionType> accountPermission = userGroup.getAccountPermissions().getPermissions().iterator();
    while (accountPermission.hasNext()) {
      jsonArray.add(accountPermission.next().toString());
    }
    jObj.add("permissions", jsonArray);
    masterObj.add("accountPermissions", jObj);

    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", account.getUuid())
        .body(masterObj, ObjectMapperType.GSON)
        .put("/userGroups/" + userGroup.getUuid() + "/permissions")
        .statusCode();
  }

  public static UserGroup updateNotificationSettings(Account account, String bearerToken, UserGroup userGroup) {
    RestResponse<UserGroup> userGroups = Setup.portal()
                                             .auth()
                                             .oauth2(bearerToken)
                                             .queryParam("accountId", account.getUuid())
                                             .body(userGroup.getNotificationSettings(), ObjectMapperType.GSON)
                                             .put("/userGroups/" + userGroup.getUuid() + "/notification-settings")
                                             .as(new GenericType<RestResponse<UserGroup>>() {}.getType());
    return userGroups.getResource();
  }

  public static UserGroup linkLDAPSettings(Account account, String bearerToken, String userGroupId,
      String ldapSettingId, LdapGroupResponse ldapGroupResponse) {
    JsonObject jObj = new JsonObject();
    jObj.addProperty("ldapGroupDN", ldapGroupResponse.getDn());
    jObj.addProperty("ldapGroupName", ldapGroupResponse.getName());
    RestResponse<UserGroup> userGroups = Setup.portal()
                                             .auth()
                                             .oauth2(bearerToken)
                                             .queryParam("accountId", account.getUuid())
                                             .body(jObj.toString())
                                             .put("/userGroups/" + userGroupId + "/link/ldap/" + ldapSettingId)
                                             .as(new GenericType<RestResponse<UserGroup>>() {}.getType());

    return userGroups.getResource();
  }

  public static UserGroup unlinkLDAPSettings(Account account, String bearerToken, String userGroupId) {
    RestResponse<UserGroup> userGroups = Setup.portal()
                                             .auth()
                                             .oauth2(bearerToken)
                                             .queryParam("accountId", account.getUuid())
                                             .queryParam("retailMembers", "undefined")
                                             .put("/userGroups/" + userGroupId + "/unlink")
                                             .as(new GenericType<RestResponse<UserGroup>>() {}.getType());

    return userGroups.getResource();
  }

  public static Boolean deleteUserGroup(Account account, String bearerToken, String userGroupId) {
    RestResponse<Boolean> userGroups = Setup.portal()
                                           .auth()
                                           .oauth2(bearerToken)
                                           .queryParam("accountId", account.getUuid())
                                           .delete("/userGroups/" + userGroupId)
                                           .as(new GenericType<RestResponse<Boolean>>() {}.getType());
    return userGroups.getResource();
  }
}

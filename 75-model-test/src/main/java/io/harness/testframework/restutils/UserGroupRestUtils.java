package io.harness.testframework.restutils;

import com.google.gson.JsonObject;

import io.harness.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;
import io.restassured.mapper.ObjectMapperType;
import software.wings.beans.Account;
import software.wings.beans.security.UserGroup;

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
}

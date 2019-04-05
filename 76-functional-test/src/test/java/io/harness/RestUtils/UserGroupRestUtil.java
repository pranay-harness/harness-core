package io.harness.RestUtils;

import com.google.gson.JsonObject;
import com.google.inject.Singleton;

import io.harness.beans.PageResponse;
import io.harness.framework.Setup;
import io.harness.rest.RestResponse;
import io.restassured.mapper.ObjectMapperType;
import software.wings.beans.Account;
import software.wings.beans.security.UserGroup;

import java.util.List;
import javax.ws.rs.core.GenericType;

@Singleton
public class UserGroupRestUtil {
  public List<UserGroup> getUserGroups(Account account, String bearerToken) {
    GenericType<RestResponse<PageResponse<UserGroup>>> userGroupType =
        new GenericType<RestResponse<PageResponse<UserGroup>>>() {};
    RestResponse<PageResponse<UserGroup>> userGroups =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", account.getUuid())
            .get("/userGroups")
            .as(new GenericType<RestResponse<PageResponse<UserGroup>>>() {}.getType());
    return userGroups.getResource();
  }

  public UserGroup createUserGroup(Account account, String bearerToken, JsonObject jsonObject) {
    RestResponse<UserGroup> userGroups = Setup.portal()
                                             .auth()
                                             .oauth2(bearerToken)
                                             .queryParam("accountId", account.getUuid())
                                             .body(jsonObject.toString())
                                             .post("/userGroups")
                                             .as(new GenericType<RestResponse<UserGroup>>() {}.getType());
    return userGroups.getResource();
  }

  public UserGroup updateNotificationSettings(Account account, String bearerToken, UserGroup userGroup) {
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

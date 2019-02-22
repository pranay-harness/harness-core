package io.harness.RestUtils;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertNotNull;

import io.harness.beans.PageResponse;
import io.harness.framework.Setup;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rest.RestResponse;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.beans.UserInvite;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.GenericType;

public class UserRestUtil extends AbstractFunctionalTest {
  public List<User> getUserList(Account account) {
    RestResponse<PageResponse<User>> userRestResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", account.getUuid())
            .get("/users")
            .as(new GenericType<RestResponse<PageResponse<User>>>() {}.getType());
    return userRestResponse.getResource().getResponse();
  }

  public List<UserInvite> inviteUser(Account account, String email) {
    UserInvite invite = new UserInvite();
    invite.setAccountId(account.getUuid());
    List<String> emailList = new ArrayList<>();
    emailList.add(email);
    invite.setEmails(emailList);
    invite.setName("Swamy Harness");
    invite.setAppId(account.getAppId());

    RestResponse<List<UserInvite>> inviteListResponse =
        given()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", account.getUuid())
            .body(invite, ObjectMapperType.GSON)
            .contentType(ContentType.JSON)
            .post("/users/invites")
            .as(new GenericType<RestResponse<List<UserInvite>>>() {}.getType());
    List<UserInvite> inviteList = inviteListResponse.getResource();
    assertNotNull(inviteList);
    return inviteList;
  }
}

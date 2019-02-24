package io.harness.RestUtils;

import static org.junit.Assert.assertNotNull;

import io.harness.beans.PageResponse;
import io.harness.framework.Registration;
import io.harness.framework.Setup;
import io.harness.framework.constants.UserConstants;
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
    invite.setName(email.replace("@guerrillamailblock.com", ""));
    invite.setAppId(account.getAppId());

    RestResponse<List<UserInvite>> inviteListResponse =
        Setup.portal()
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

  public UserInvite completeUserRegistration(Account account, UserInvite invite) {
    Registration registration = new Registration();
    registration.setAccountId(account.getAccountKey());
    registration.setAgreement(true);
    registration.setEmail(invite.getEmail());
    registration.setName(invite.getName());
    registration.setPassword(UserConstants.DEFAULT_PASSWORD);
    registration.setUuid(invite.getUuid());

    RestResponse<UserInvite> completed = Setup.portal()
                                             .auth()
                                             .oauth2(bearerToken)
                                             .queryParam("accountId", account.getUuid())
                                             .body(registration, ObjectMapperType.GSON)
                                             .contentType(ContentType.JSON)
                                             .put("/users/invites/" + invite.getUuid())
                                             .as(new GenericType<RestResponse<UserInvite>>() {}.getType());

    return completed.getResource();
  }
}

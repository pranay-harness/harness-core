package io.harness.restutils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.harness.framework.Setup;
import io.harness.framework.email.GuerillaEmailDetails;
import io.harness.framework.email.GuerillaEmailInfo;
import io.harness.framework.email.GuerillaIndividualEmail;

public class GuerillaMailUtils {
  public static GuerillaEmailInfo refreshAndGetNewEmail() {
    GuerillaEmailInfo emailInfo = Setup.email()
                                      .queryParam("f", "get_email_address")
                                      .queryParam("ip", "127.0.0.1")
                                      .queryParam("agent", "Mozilla_foo_bar")
                                      .get()
                                      .as(GuerillaEmailInfo.class);
    assertNotNull(emailInfo);
    boolean isEmailForgotten = forgetEmailId(emailInfo.getSidToken());
    assertTrue("Created email may be an existing one. Deletion failed : " + emailInfo.getEmailAddr(), isEmailForgotten);
    emailInfo = Setup.email()
                    .queryParam("f", "get_email_address")
                    .queryParam("ip", "127.0.0.1")
                    .queryParam("agent", "Mozilla_foo_bar")
                    .get()
                    .as(GuerillaEmailInfo.class);

    assertNotNull(emailInfo);

    return emailInfo;
  }

  public static boolean forgetEmailId(String sidToken) {
    return Setup.email().queryParam("f", "forget_me").queryParam("sid_token", sidToken).get().as(Boolean.class);
  }

  public static GuerillaEmailDetails getAllEmailList(String sidToken) {
    GuerillaEmailDetails currentEmailList = Setup.email()
                                                .queryParam("f", "get_email_list")
                                                .queryParam("offset", "0")
                                                .queryParam("sid_token", sidToken)
                                                .get()
                                                .as(GuerillaEmailDetails.class);

    assertNotNull(currentEmailList);
    return currentEmailList;
  }

  public static GuerillaIndividualEmail fetchEmail(String sidToken, String mailId) {
    GuerillaIndividualEmail email = Setup.email()
                                        .queryParam("f", "fetch_email")
                                        .queryParam("email_id", mailId)
                                        .queryParam("sid_token", sidToken)
                                        .get()
                                        .as(GuerillaIndividualEmail.class);
    assertNotNull(email);
    return email;
  }
}

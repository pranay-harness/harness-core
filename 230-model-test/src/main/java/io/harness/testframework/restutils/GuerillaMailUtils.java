/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.testframework.restutils;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.email.GuerillaEmailDetails;
import io.harness.testframework.framework.email.GuerillaEmailInfo;
import io.harness.testframework.framework.email.GuerillaIndividualEmail;

public class GuerillaMailUtils {
  public static GuerillaEmailInfo refreshAndGetNewEmail() {
    GuerillaEmailInfo emailInfo = Setup.email()
                                      .queryParam("f", "get_email_address")
                                      .queryParam("ip", "127.0.0.1")
                                      .queryParam("agent", "Mozilla_foo_bar")
                                      .get()
                                      .as(GuerillaEmailInfo.class);
    assertThat(emailInfo).isNotNull();
    boolean isEmailForgotten = forgetEmailId(emailInfo.getSidToken());
    assertThat(isEmailForgotten).isTrue();
    emailInfo = Setup.email()
                    .queryParam("f", "get_email_address")
                    .queryParam("ip", "127.0.0.1")
                    .queryParam("agent", "Mozilla_foo_bar")
                    .get()
                    .as(GuerillaEmailInfo.class);

    assertThat(emailInfo).isNotNull();

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

    assertThat(currentEmailList).isNotNull();
    return currentEmailList;
  }

  public static GuerillaIndividualEmail fetchEmail(String sidToken, String mailId) {
    GuerillaIndividualEmail email = Setup.email()
                                        .queryParam("f", "fetch_email")
                                        .queryParam("email_id", mailId)
                                        .queryParam("sid_token", sidToken)
                                        .get()
                                        .as(GuerillaIndividualEmail.class);
    assertThat(email).isNotNull();
    return email;
  }
}

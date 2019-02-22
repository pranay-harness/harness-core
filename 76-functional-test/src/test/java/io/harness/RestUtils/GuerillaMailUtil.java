package io.harness.RestUtils;

import io.harness.framework.GuerillaEmailInfo;
import io.harness.framework.Setup;

public class GuerillaMailUtil {
  public GuerillaEmailInfo getNewEmailId() {
    GuerillaEmailInfo emailInfo = Setup.email()
                                      .queryParam("f", "get_email_address")
                                      .queryParam("ip", "127.0.0.1")
                                      .queryParam("agent", "Mozilla_foo_bar")
                                      .get()
                                      .as(GuerillaEmailInfo.class);

    return emailInfo;
  }

  public boolean forgetEmailId(String sidToken) {
    return Setup.email().queryParam("f", "forget_me").queryParam("sid_token", sidToken).get().as(Boolean.class);
  }
}

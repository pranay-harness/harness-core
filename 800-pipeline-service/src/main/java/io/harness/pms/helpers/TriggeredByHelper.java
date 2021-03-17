package io.harness.pms.helpers;

import io.harness.beans.EmbeddedUser;
import io.harness.pms.contracts.plan.TriggeredBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class TriggeredByHelper {
  @Inject private CurrentUserHelper currentUserHelper;

  public TriggeredBy getFromSecurityContext() {
    EmbeddedUser user = currentUserHelper.getFromSecurityContext();
    return TriggeredBy.newBuilder()
        .setUuid(user.getUuid())
        .setIdentifier(user.getName())
        .putExtraInfo("email", user.getEmail())
        .build();
  }
}

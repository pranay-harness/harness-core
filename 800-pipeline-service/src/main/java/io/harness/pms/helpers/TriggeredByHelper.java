package io.harness.pms.helpers;

import static io.harness.security.dto.PrincipalType.USER;

import io.harness.PipelineServiceConfiguration;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.user.User;
import io.harness.ng.core.user.remote.UserClient;
import io.harness.pms.contracts.plan.TriggeredBy;
import io.harness.remote.client.RestClientUtils;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.UserPrincipal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.List;

@Singleton
public class TriggeredByHelper {
  private static final TriggeredBy DEFAULT_TRIGGERED_BY = TriggeredBy.newBuilder()
                                                              .setUuid("lv0euRhKRCyiXWzS7pOg6g")
                                                              .putExtraInfo("email", "admin@harness.io")
                                                              .setIdentifier("Admin")
                                                              .build();

  @Inject private PipelineServiceConfiguration configuration;
  @Inject private UserClient userClient;

  public TriggeredBy getFromSecurityContext() {
    if (!configuration.isEnableAuth()) {
      return DEFAULT_TRIGGERED_BY;
    }
    if (SecurityContextBuilder.getPrincipal() == null
        || !USER.equals(SecurityContextBuilder.getPrincipal().getType())) {
      throw new InvalidRequestException("Unable to fetch triggering user");
    }

    UserPrincipal userPrincipal = (UserPrincipal) SecurityContextBuilder.getPrincipal();
    String userId = userPrincipal.getName();
    List<User> users = RestClientUtils.getResponse(userClient.getUsersByIds(Collections.singletonList(userId)));
    if (EmptyPredicate.isEmpty(users)) {
      throw new InvalidRequestException(String.format("Invalid user: %s", userId));
    }

    User user = users.get(0);
    return TriggeredBy.newBuilder()
        .setUuid(userId)
        .setIdentifier(user.getName())
        .putExtraInfo("email", user.getEmail())
        .build();
  }
}

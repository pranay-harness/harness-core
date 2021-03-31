package io.harness.pms.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.security.dto.PrincipalType.USER;

import io.harness.PipelineServiceConfiguration;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.remote.UserClient;
import io.harness.remote.client.RestClientUtils;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.UserPrincipal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;

@Singleton
@OwnedBy(PIPELINE)
public class CurrentUserHelper {
  private static final EmbeddedUser DEFAULT_EMBEDDED_USER =
      EmbeddedUser.builder().uuid("lv0euRhKRCyiXWzS7pOg6g").name("Admin").email("admin@harness.io").build();

  @Inject private PipelineServiceConfiguration configuration;
  @Inject private UserClient userClient;

  public EmbeddedUser getFromSecurityContext() {
    if (!configuration.isEnableAuth()) {
      return DEFAULT_EMBEDDED_USER;
    }
    if (SourcePrincipalContextBuilder.getSourcePrincipal() == null
        || !USER.equals(SourcePrincipalContextBuilder.getSourcePrincipal().getType())) {
      throw new InvalidRequestException("Unable to fetch current user");
    }

    UserPrincipal userPrincipal = (UserPrincipal) SourcePrincipalContextBuilder.getSourcePrincipal();
    String userId = userPrincipal.getName();
    Optional<UserInfo> userOptional = RestClientUtils.getResponse(userClient.getUserById(userId));
    if (!userOptional.isPresent()) {
      throw new InvalidRequestException(String.format("Invalid user: %s", userId));
    }
    UserInfo user = userOptional.get();
    return EmbeddedUser.builder().uuid(user.getUuid()).name(user.getName()).email(user.getEmail()).build();
  }
}

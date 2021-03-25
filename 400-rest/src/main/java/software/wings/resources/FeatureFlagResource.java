package software.wings.resources;

import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.exception.WingsException.USER;

import io.harness.beans.FeatureFlag;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.UnauthorizedException;
import io.harness.ff.FeatureFlagService;
import io.harness.rest.RestResponse;

import software.wings.beans.User;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.HarnessUserGroupService;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;

@Api("feature-flag")
@Path("/feature-flag")
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class FeatureFlagResource {
  private final FeatureFlagService featureFlagService;
  private final HarnessUserGroupService harnessUserGroupService;

  @Inject
  public FeatureFlagResource(FeatureFlagService featureFlagService, HarnessUserGroupService harnessUserGroupService) {
    this.featureFlagService = featureFlagService;
    this.harnessUserGroupService = harnessUserGroupService;
  }

  @GET
  @Path("globally-enabled-feature-flags")
  public RestResponse<List<FeatureFlag>> getGloballyEnabledFlags() {
    User user = UserThreadLocal.get();

    if (user == null) {
      throw new InvalidArgumentsException("Invalid User");
    }
    if (hasNone(user.getEmail()) || !user.getEmail().endsWith("@harness.io")) {
      throw new UnauthorizedException("User not authorized.", USER);
    }

    if (harnessUserGroupService.isHarnessSupportUser(user.getUuid())) {
      return new RestResponse<>(featureFlagService.getGloballyEnabledFeatureFlags());
    } else {
      return RestResponse.Builder.aRestResponse()
          .withResponseMessages(Lists.newArrayList(
              ResponseMessage.builder().message("User not allowed get list of feature flags ").build()))
          .build();
    }
  }
}

package software.wings.resources;

import static software.wings.security.PermissionAttribute.ResourceType.DELEGATE;

import io.harness.delegate.beans.DelegateHeartbeatDetails;
import io.harness.delegate.beans.DelegateInitializationDetails;
import io.harness.rest.RestResponse;

import software.wings.security.annotations.Scope;
import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.validator.constraints.NotEmpty;

@Api("delegates-verification")
@Path("/delegates-verification")
@Produces(MediaType.APPLICATION_JSON)
@Scope(DELEGATE)
@Slf4j
public class DelegateVerificationNgResource {
  private final DelegateService delegateService;

  @Inject
  public DelegateVerificationNgResource(DelegateService delegateService) {
    this.delegateService = delegateService;
  }

  @GET
  @Path("/heartbeat")
  public RestResponse<DelegateHeartbeatDetails> getDelegatesHeartbeatDetails(
      @QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("sessionId") @NotEmpty String sessionIdentifier) {
    List<String> registeredDelegateIds = delegateService.obtainDelegateIds(accountId, sessionIdentifier);

    if (CollectionUtils.isNotEmpty(registeredDelegateIds)) {
      List<String> connectedDelegates = delegateService.getConnectedDelegates(accountId, registeredDelegateIds);

      return new RestResponse<>(DelegateHeartbeatDetails.builder()
                                    .numberOfRegisteredDelegates(registeredDelegateIds.size())
                                    .numberOfConnectedDelegates(connectedDelegates.size())
                                    .build());
    }

    return new RestResponse<>(DelegateHeartbeatDetails.builder().build());
  }

  @GET
  @Path("/initialized")
  public RestResponse<List<DelegateInitializationDetails>> getDelegatesInitializationDetails(
      @QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("sessionId") @NotEmpty String sessionIdentifier) {
    List<String> registeredDelegateIds = delegateService.obtainDelegateIds(accountId, sessionIdentifier);

    if (CollectionUtils.isNotEmpty(registeredDelegateIds)) {
      return new RestResponse<>(delegateService.obtainDelegateInitializationDetails(accountId, registeredDelegateIds));
    }

    return new RestResponse<>(Collections.emptyList());
  }
}

package software.wings.resources;

import static software.wings.security.PermissionAttribute.ResourceType.USER;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.ccm.communication.CECommunicationsService;
import io.harness.ccm.communication.entities.CECommunications;
import io.harness.ccm.communication.entities.CommunicationType;
import io.harness.exception.InvalidRequestException;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import software.wings.beans.User;
import software.wings.security.UserThreadLocal;
import software.wings.security.annotations.Scope;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("ceCommunications")
@Path("/ceCommunications")
@Produces("application/json")
@Scope(USER)
public class CECommunicationsResource {
  private CECommunicationsService communicationsService;

  @Inject
  public CECommunicationsResource(CECommunicationsService communicationsService) {
    this.communicationsService = communicationsService;
  }

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<List<CECommunications>> get(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(communicationsService.list(accountId, getUserEmail()));
  }

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse update(@QueryParam("accountId") String accountId, @QueryParam("type") CommunicationType type,
      @QueryParam("enable") boolean enable) {
    communicationsService.update(accountId, getUserEmail(), type, enable);
    return new RestResponse();
  }

  private String getUserEmail() {
    User existingUser = UserThreadLocal.get();
    if (existingUser == null) {
      throw new InvalidRequestException("Invalid User");
    }
    return existingUser.getEmail();
  }
}

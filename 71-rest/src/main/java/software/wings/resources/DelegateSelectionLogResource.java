package software.wings.resources;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.delegate.beans.DelegateSelectionLogParams;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.DelegateSelectionLogsService;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("selection-logs")
@Path("/selection-logs")
@Produces("application/json")
@Scope(APPLICATION)
@AuthRule(permissionType = LOGGED_IN)
public class DelegateSelectionLogResource {
  private DelegateSelectionLogsService delegateSelectionLogsService;

  @Inject
  public DelegateSelectionLogResource(DelegateSelectionLogsService delegateSelectionLogsService) {
    this.delegateSelectionLogsService = delegateSelectionLogsService;
  }

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<List<DelegateSelectionLogParams>> getSelectionLogs(
      @QueryParam("accountId") String accountId, @QueryParam("taskId") String taskId) {
    return new RestResponse(delegateSelectionLogsService.fetchTaskSelectionLogs(accountId, taskId));
  }
}

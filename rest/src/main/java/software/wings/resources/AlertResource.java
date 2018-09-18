package software.wings.resources;

import static software.wings.security.PermissionAttribute.ResourceType.ROLE;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.alert.Alert;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.AlertService;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by brett on 10/18/17
 */
@Api("alerts")
@Path("/alerts")
@Produces("application/json")
@Scope(ROLE)
public class AlertResource {
  private AlertService alertService;

  @Inject
  public AlertResource(AlertService alertService) {
    this.alertService = alertService;
  }

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Alert>> list(
      @QueryParam("accountId") String accountId, @BeanParam PageRequest<Alert> request) {
    return new RestResponse<>(alertService.list(request));
  }
}

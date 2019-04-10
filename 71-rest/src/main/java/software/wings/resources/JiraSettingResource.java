package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.rest.RestResponse;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.service.impl.JiraHelperService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Path("/jirasetting")
@Produces("application/json")
public class JiraSettingResource {
  @Inject JiraHelperService jiraHelperService;

  /**
   * List.
   *
   * @param accountId the account id
   * @param connectorId   the request
   * @return the rest response
   */
  @GET
  @Path("{connectorId}/projects")
  @Timed
  @ExceptionMetered
  public RestResponse getProjects(@QueryParam("appId") String appId,
      @QueryParam("accountId") @NotEmpty String accountId, @PathParam("connectorId") String connectorId) {
    return new RestResponse<>(jiraHelperService.getProjects(connectorId, accountId, appId));
  }

  /**
   * List.
   *
   * @param accountId the account id
   * @param connectorId   the request
   * @return the rest response
   */
  @GET
  @Path("{connectorId}/field_options")
  @Timed
  @ExceptionMetered
  public RestResponse getFields(@QueryParam("appId") String appId, @QueryParam("accountId") @NotEmpty String accountId,
      @PathParam("connectorId") String connectorId, @QueryParam("project") String project) {
    return new RestResponse<>(jiraHelperService.getFieldOptions(connectorId, project, accountId, appId));
  }

  /**
   * List.
   *
   * @param accountId the account id
   * @param connectorId   the request
   * @return the rest response
   */
  @GET
  @Path("{connectorId}/statuses")
  @Timed
  @ExceptionMetered
  public RestResponse getGeneric(@QueryParam("appId") String appId, @QueryParam("accountId") @NotEmpty String accountId,
      @PathParam("connectorId") String connectorId, @QueryParam("project") String project) {
    return new RestResponse<>(jiraHelperService.getStatuses(connectorId, project, accountId, appId));
  }

  /**
   * List.
   *
   * @param accountId the account id
   * @param connectorId   the request
   * @return the rest response
   */
  @GET
  @Path("{connectorId}/createmeta")
  @Timed
  @ExceptionMetered
  public RestResponse getCreateMetadata(@QueryParam("appId") String appId,
      @QueryParam("accountId") @NotEmpty String accountId, @PathParam("connectorId") String connectorId,
      @QueryParam("expand") String expand, @QueryParam("project") String project) {
    return new RestResponse<>(jiraHelperService.getCreateMetadata(connectorId, expand, project, accountId, appId));
  }
}

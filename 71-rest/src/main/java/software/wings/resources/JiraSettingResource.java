package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.JiraConfig;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute;
import software.wings.service.impl.JiraHelperService;
import software.wings.service.intfc.SettingsService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Path("/jirasetting")
@Produces("application/json")
public class JiraSettingResource {
  @Inject JiraHelperService jiraHelperService;
  @Inject SettingsService settingService;

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
    SettingAttribute jiraSettingAttribute = settingService.get(connectorId);
    return new RestResponse<>(
        jiraHelperService.getProjects((JiraConfig) jiraSettingAttribute.getValue(), accountId, appId));
  }

  /**
   * List.
   *
   * @param accountId the account id
   * @param connectorId   the request
   * @return the rest response
   */
  @GET
  @Path("{connectorId}/fields")
  @Timed
  @ExceptionMetered
  public RestResponse getFields(@QueryParam("appId") String appId, @QueryParam("accountId") @NotEmpty String accountId,
      @PathParam("connectorId") String connectorId) {
    SettingAttribute jiraSettingAttribute = settingService.get(connectorId);
    return new RestResponse<>(
        jiraHelperService.getFields((JiraConfig) jiraSettingAttribute.getValue(), accountId, appId));
  }
}

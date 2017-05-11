package software.wings.resources;

import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.PublicApi;
import software.wings.service.impl.appdynamics.AppdynamicsApplicationResponse;
import software.wings.service.intfc.appdynamics.AppdynamicsService;
import software.wings.service.intfc.SettingsService;

import java.io.IOException;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by rsingh on 4/14/17.
 */
@Api("appdynamics")
@Path("/appdynamics")
@Produces("application/json")
@AuthRule(ResourceType.SETTING)
public class AppdynamicsResource {
  @Inject private SettingsService settingsService;

  @Inject private AppdynamicsService appdynamicsService;

  @GET
  @Path("/applications")
  @Timed
  @ExceptionMetered
  public RestResponse<List<AppdynamicsApplicationResponse>> getAllApplications(
      @QueryParam("accountId") String accountId, @QueryParam("settingId") final String settingId) throws IOException {
    final SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    return new RestResponse<>(appdynamicsService.getApplications(settingAttribute));
  }
}

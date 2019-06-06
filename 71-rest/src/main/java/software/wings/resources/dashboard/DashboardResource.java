package software.wings.resources.dashboard;

import static software.wings.security.PermissionAttribute.ResourceType.CUSTOM_DASHBOARD;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.dashboard.DashboardSettings;
import io.harness.dashboard.DashboardSettingsService;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import org.hibernate.validator.constraints.NotBlank;
import software.wings.beans.Application;
import software.wings.beans.FeatureName;
import software.wings.security.annotations.ListAPI;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.FeatureFlagService;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Api("custom-dashboard")
@Path("/custom-dashboard")
@Scope(CUSTOM_DASHBOARD)
@Produces(MediaType.APPLICATION_JSON)
public class DashboardResource {
  private DashboardSettingsService dashboardSettingsService;
  private FeatureFlagService featureFlagService;

  @Inject
  public DashboardResource(DashboardSettingsService dashboardSettingsService, FeatureFlagService featureFlagService) {
    this.dashboardSettingsService = dashboardSettingsService;
    this.featureFlagService = featureFlagService;
  }

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<DashboardSettings> createDashboardSetting(
      @QueryParam("accountId") @NotBlank String accountId, DashboardSettings settings) {
    if (!featureFlagService.isEnabled(FeatureName.CUSTOM_DASHBOARD, settings.getAccountId())) {
      throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED);
    }
    settings.setAccountId(accountId);
    return new RestResponse<DashboardSettings>(dashboardSettingsService.createDashboardSettings(accountId, settings));
  }

  @PUT
  @Timed
  @ExceptionMetered
  public RestResponse<DashboardSettings> updateDashboardSettings(
      @QueryParam("accountId") @NotBlank String accountId, DashboardSettings settings) {
    if (!featureFlagService.isEnabled(FeatureName.CUSTOM_DASHBOARD, settings.getAccountId())) {
      throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED);
    }
    settings.setAccountId(accountId);
    return new RestResponse<DashboardSettings>(dashboardSettingsService.updateDashboardSettings(accountId, settings));
  }

  @DELETE
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> deleteDashboardSettings(
      @QueryParam("accountId") @NotBlank String accountId, @QueryParam("dashboardId") @NotBlank String id) {
    if (!featureFlagService.isEnabled(FeatureName.CUSTOM_DASHBOARD, accountId)) {
      throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED);
    }
    return new RestResponse<Boolean>(dashboardSettingsService.deleteDashboardSettings(accountId, id));
  }

  @GET
  @Timed
  @ListAPI(CUSTOM_DASHBOARD)
  @ExceptionMetered
  public RestResponse<PageResponse<DashboardSettings>> getDashboardSettings(
      @QueryParam("accountId") @NotBlank String accountId, @BeanParam PageRequest<Application> pageRequest) {
    if (!featureFlagService.isEnabled(FeatureName.CUSTOM_DASHBOARD, accountId)) {
      throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED);
    }
    return new RestResponse<PageResponse<DashboardSettings>>(
        dashboardSettingsService.getDashboardSettingSummary(accountId, pageRequest));
  }

  @GET
  @Timed
  @Path("{dashboardId}")
  @ExceptionMetered
  public RestResponse<DashboardSettings> getDashboardSetting(
      @QueryParam("accountId") @NotBlank String accountId, @PathParam("dashboardId") String dashboardId) {
    if (!featureFlagService.isEnabled(FeatureName.CUSTOM_DASHBOARD, accountId)) {
      throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED);
    }
    return new RestResponse<DashboardSettings>(dashboardSettingsService.get(accountId, dashboardId));
  }
}

package software.wings.resources.dashboard;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static software.wings.security.PermissionAttribute.ResourceType.CUSTOM_DASHBOARD;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.PageResponse.PageResponseBuilder;
import io.harness.beans.SearchFilter.Operator;
import io.harness.dashboard.Action;
import io.harness.dashboard.DashboardSettings;
import io.harness.dashboard.DashboardSettingsService;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import org.hibernate.validator.constraints.NotBlank;
import software.wings.beans.Application;
import software.wings.beans.FeatureName;
import software.wings.features.CustomDashboardFeature;
import software.wings.features.api.AccountId;
import software.wings.features.api.RestrictedApi;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.ListAPI;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.security.auth.DashboardAuthHandler;
import software.wings.service.intfc.FeatureFlagService;

import java.util.Collections;
import java.util.Set;
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
public class CustomDashboardResource {
  private DashboardSettingsService dashboardSettingsService;
  private FeatureFlagService featureFlagService;
  private DashboardAuthHandler dashboardAuthHandler;

  @Inject
  public CustomDashboardResource(DashboardSettingsService dashboardSettingsService,
      FeatureFlagService featureFlagService, DashboardAuthHandler dashboardAuthHandler) {
    this.dashboardSettingsService = dashboardSettingsService;
    this.featureFlagService = featureFlagService;
    this.dashboardAuthHandler = dashboardAuthHandler;
  }

  @POST
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
  @RestrictedApi(CustomDashboardFeature.class)
  public RestResponse<DashboardSettings> createDashboardSetting(
      @QueryParam("accountId") @NotBlank @AccountId String accountId, DashboardSettings settings) {
    if (!featureFlagService.isEnabled(FeatureName.CUSTOM_DASHBOARD, settings.getAccountId())) {
      throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED);
    }
    settings.setAccountId(accountId);
    return new RestResponse<>(dashboardSettingsService.createDashboardSettings(accountId, settings));
  }

  @PUT
  @Timed
  @ExceptionMetered
  @RestrictedApi(CustomDashboardFeature.class)
  public RestResponse<DashboardSettings> updateDashboardSettings(
      @QueryParam("accountId") @NotBlank @AccountId String accountId, DashboardSettings settings) {
    if (!featureFlagService.isEnabled(FeatureName.CUSTOM_DASHBOARD, settings.getAccountId())) {
      throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED, USER);
    }

    DashboardSettings existingDashboardSetting = dashboardSettingsService.get(accountId, settings.getUuid());
    dashboardAuthHandler.authorize(existingDashboardSetting, accountId, Action.UPDATE);
    settings.setAccountId(accountId);
    return new RestResponse<>(dashboardSettingsService.updateDashboardSettings(accountId, settings));
  }

  @DELETE
  @Timed
  @ExceptionMetered
  @RestrictedApi(CustomDashboardFeature.class)
  public RestResponse<Boolean> deleteDashboardSettings(
      @QueryParam("accountId") @NotBlank @AccountId String accountId, @QueryParam("dashboardId") @NotBlank String id) {
    if (!featureFlagService.isEnabled(FeatureName.CUSTOM_DASHBOARD, accountId)) {
      throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED, USER);
    }

    DashboardSettings existingDashboardSetting = dashboardSettingsService.get(accountId, id);
    dashboardAuthHandler.authorize(existingDashboardSetting, accountId, Action.DELETE);
    return new RestResponse<>(dashboardSettingsService.deleteDashboardSettings(accountId, id));
  }

  @GET
  @Timed
  @ListAPI(CUSTOM_DASHBOARD)
  @ExceptionMetered
  public RestResponse<PageResponse<DashboardSettings>> getDashboardSettings(
      @QueryParam("accountId") @NotBlank String accountId, @BeanParam PageRequest<Application> pageRequest) {
    if (!featureFlagService.isEnabled(FeatureName.CUSTOM_DASHBOARD, accountId)) {
      throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED, USER);
    }
    Set<String> allowedDashboardSettingIds = dashboardAuthHandler.getAllowedDashboardSettingIds();

    if (isEmpty(allowedDashboardSettingIds)) {
      return new RestResponse<>(
          PageResponseBuilder.aPageResponse().withTotal(0).withResponse(Collections.emptyList()).build());
    }

    pageRequest.addFilter("_id", Operator.IN, allowedDashboardSettingIds.toArray());
    return new RestResponse<>(dashboardSettingsService.getDashboardSettingSummary(accountId, pageRequest));
  }

  @GET
  @Timed
  @Path("{dashboardId}")
  @ExceptionMetered
  @RestrictedApi(CustomDashboardFeature.class)
  public RestResponse<DashboardSettings> getDashboardSetting(
      @QueryParam("accountId") @NotBlank @AccountId String accountId, @PathParam("dashboardId") String dashboardId) {
    if (!featureFlagService.isEnabled(FeatureName.CUSTOM_DASHBOARD, accountId)) {
      throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED);
    }

    DashboardSettings dashboardSetting = dashboardSettingsService.get(accountId, dashboardId);
    dashboardAuthHandler.authorize(dashboardSetting, accountId, Action.READ);
    return new RestResponse<>(dashboardSetting);
  }
}

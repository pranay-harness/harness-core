package software.wings.resources.dashboard;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.security.PermissionAttribute.ResourceType.CUSTOM_DASHBOARD;

import com.google.common.collect.Lists;
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
import io.harness.eraro.Level;
import io.harness.eraro.ResponseMessage;
import io.harness.event.reconciliation.deployment.ReconciliationStatus;
import io.harness.event.reconciliation.service.DeploymentReconService;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import io.harness.rest.RestResponse;
import io.harness.rest.RestResponse.Builder;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.FeatureName;
import software.wings.beans.User;
import software.wings.features.api.AccountId;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.UserThreadLocal;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.ListAPI;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.DashboardLogContext;
import software.wings.service.impl.security.auth.DashboardAuthHandler;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.HarnessUserGroupService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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

@Slf4j
@Api("custom-dashboard")
@Path("/custom-dashboard")
@Scope(CUSTOM_DASHBOARD)
@Produces(MediaType.APPLICATION_JSON)
public class CustomDashboardResource {
  private DashboardSettingsService dashboardSettingsService;
  private FeatureFlagService featureFlagService;
  private DashboardAuthHandler dashboardAuthHandler;
  private HarnessUserGroupService harnessUserGroupService;
  private DeploymentReconService deploymentReconService;
  private AccountService accountService;

  @Inject
  public CustomDashboardResource(DashboardSettingsService dashboardSettingsService,
      FeatureFlagService featureFlagService, DashboardAuthHandler dashboardAuthHandler,
      HarnessUserGroupService harnessUserGroupService, DeploymentReconService deploymentReconService,
      AccountService accountService) {
    this.dashboardSettingsService = dashboardSettingsService;
    this.featureFlagService = featureFlagService;
    this.dashboardAuthHandler = dashboardAuthHandler;
    this.harnessUserGroupService = harnessUserGroupService;
    this.deploymentReconService = deploymentReconService;
    this.accountService = accountService;
  }

  @POST
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
  public RestResponse<DashboardSettings> createDashboardSetting(
      @QueryParam("accountId") @NotBlank @AccountId String accountId, DashboardSettings settings) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      if (!featureFlagService.isEnabled(FeatureName.CUSTOM_DASHBOARD, settings.getAccountId())) {
        throw new InvalidRequestException("User not authorized", USER);
      }
      settings.setAccountId(accountId);
      return new RestResponse<>(dashboardSettingsService.createDashboardSettings(accountId, settings));
    }
  }

  @PUT
  @Timed
  @ExceptionMetered
  public RestResponse<DashboardSettings> updateDashboardSettings(
      @QueryParam("accountId") @NotBlank @AccountId String accountId, DashboardSettings settings) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DashboardLogContext(settings.getUuid(), OVERRIDE_ERROR)) {
      if (!featureFlagService.isEnabled(FeatureName.CUSTOM_DASHBOARD, settings.getAccountId())) {
        throw new InvalidRequestException("User not authorized", USER);
      }

      DashboardSettings existingDashboardSetting = dashboardSettingsService.get(accountId, settings.getUuid());
      dashboardAuthHandler.authorize(existingDashboardSetting, accountId, Action.UPDATE);
      settings.setAccountId(accountId);
      return new RestResponse<>(dashboardSettingsService.updateDashboardSettings(accountId, settings));
    }
  }

  @DELETE
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> deleteDashboardSettings(
      @QueryParam("accountId") @NotBlank @AccountId String accountId, @QueryParam("dashboardId") @NotBlank String id) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DashboardLogContext(id, OVERRIDE_ERROR)) {
      if (!featureFlagService.isEnabled(FeatureName.CUSTOM_DASHBOARD, accountId)) {
        throw new InvalidRequestException("User not authorized", USER);
      }

      DashboardSettings existingDashboardSetting = dashboardSettingsService.get(accountId, id);
      dashboardAuthHandler.authorize(existingDashboardSetting, accountId, Action.DELETE);
      return new RestResponse<>(dashboardSettingsService.deleteDashboardSettings(accountId, id));
    }
  }

  @GET
  @Timed
  @ListAPI(CUSTOM_DASHBOARD)
  @ExceptionMetered
  public RestResponse<PageResponse<DashboardSettings>> getDashboardSettings(
      @QueryParam("accountId") @NotBlank String accountId, @BeanParam PageRequest<Application> pageRequest) {
    if (!featureFlagService.isEnabled(FeatureName.CUSTOM_DASHBOARD, accountId)) {
      throw new InvalidRequestException("User not authorized", USER);
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
  public RestResponse<DashboardSettings> getDashboardSetting(
      @QueryParam("accountId") @NotBlank @AccountId String accountId, @PathParam("dashboardId") String dashboardId) {
    if (!featureFlagService.isEnabled(FeatureName.CUSTOM_DASHBOARD, accountId)) {
      throw new InvalidRequestException("User not authorized", USER);
    }

    DashboardSettings dashboardSetting = dashboardSettingsService.get(accountId, dashboardId);
    dashboardAuthHandler.authorize(dashboardSetting, accountId, Action.READ);
    return new RestResponse<>(dashboardSetting);
  }

  /**
   * Perform reconciliation
   *
   * @return the rest response
   */
  @PUT
  @Path("deployment-recon-per-account")
  @Scope(value = ResourceType.USER, scope = LOGGED_IN)
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse performReconciliationSingleAccount(
      @QueryParam("targetAccountId") @NotEmpty String targetAccountId,
      @QueryParam("durationStartTs") Long durationStartTs, @QueryParam("durationEndTs") Long durationEndTs) {
    User authUser = UserThreadLocal.get();

    if (harnessUserGroupService.isHarnessSupportUser(authUser.getUuid())) {
      if (durationEndTs == null || durationStartTs == null || durationStartTs <= 0 || durationEndTs <= 0) {
        return Builder.aRestResponse()
            .withResponseMessages(Lists.newArrayList(ResponseMessage.builder()
                                                         .message("durationStartTs or endTs is null or invalid")

                                                         .build()))
            .build();
      }

      Account account = accountService.get(targetAccountId);
      if (account == null) {
        return Builder.aRestResponse()
            .withResponseMessages(Lists.newArrayList(ResponseMessage.builder()
                                                         .message(targetAccountId + " not found")
                                                         .code(ErrorCode.INVALID_ARGUMENT)
                                                         .build()))
            .build();
      }
      ReconciliationStatus status =
          deploymentReconService.performReconciliation(targetAccountId, durationStartTs, durationEndTs);
      return Builder.aRestResponse()
          .withResponseMessages(Lists.newArrayList(ResponseMessage.builder()
                                                       .message(targetAccountId + ":" + status.name())
                                                       .code(null)
                                                       .level(Level.INFO)
                                                       .build()))
          .build();
    } else {
      return Builder.aRestResponse()
          .withResponseMessages(
              Lists.newArrayList(ResponseMessage.builder()
                                     .message("User not allowed to perform the deployment-recon-per-account operation")
                                     .build()))
          .build();
    }
  }

  /**
   * Perform reconciliation
   *
   * @return the rest response
   */
  @PUT
  @Path("deployment-recon-all-accounts")
  @Scope(value = ResourceType.USER, scope = LOGGED_IN)
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse performReconciliationAllAccounts(
      @QueryParam("durationStartTs") Long durationStartTs, @QueryParam("durationEndTs") Long durationEndTs) {
    User authUser = UserThreadLocal.get();
    if (harnessUserGroupService.isHarnessSupportUser(authUser.getUuid())) {
      if (durationEndTs == null || durationStartTs == null || durationStartTs <= 0 || durationEndTs <= 0) {
        return Builder.aRestResponse()
            .withResponseMessages(Lists.newArrayList(ResponseMessage.builder()
                                                         .message("durationStartTs or endTs is null or invalid")
                                                         .code(ErrorCode.INVALID_ARGUMENT)
                                                         .build()))
            .build();
      }

      List<Account> accountList = accountService.listAllAccountWithDefaultsWithoutLicenseInfo();
      Map<String, String> accountReconStatusMap = new HashMap<>();
      for (Account account : accountList) {
        ReconciliationStatus status =
            deploymentReconService.performReconciliation(account.getUuid(), durationStartTs, durationEndTs);
        accountReconStatusMap.put(account.getAccountName(), status.name());
        logger.info("Reconcilation completed for accountID:[{}],accountName:[{}],status:[{}]", account.getUuid(),
            account.getAccountName(), status);
      }
      return Builder.aRestResponse()
          .withResponseMessages(accountReconStatusMap.entrySet()
                                    .stream()
                                    .map(stringStringEntry
                                        -> ResponseMessage.builder()
                                               .message(stringStringEntry.getKey() + ":" + stringStringEntry.getValue())
                                               .code(null)
                                               .level(Level.INFO)
                                               .build())
                                    .collect(Collectors.toList()))
          .build();
    } else {
      return Builder.aRestResponse()
          .withResponseMessages(
              Lists.newArrayList(ResponseMessage.builder()
                                     .message("User not allowed to perform the deployment-recon-all-account operation")
                                     .build()))
          .build();
    }
  }
}

package software.wings.resources;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.utils.Utils.urlDecode;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.dropwizard.jersey.PATCH;
import io.harness.account.ProvisionStep;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnauthorizedException;
import io.harness.exception.WingsException;
import io.harness.logging.AutoLogContext;
import io.harness.marketplace.gcp.GcpMarketPlaceApiHandler;
import io.harness.persistence.AccountLogContext;
import io.harness.rest.RestResponse;
import io.harness.scheduler.PersistentScheduler;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.http.Body;
import software.wings.beans.Account;
import software.wings.beans.AccountEvent;
import software.wings.beans.AccountMigration;
import software.wings.beans.AccountSalesContactsInfo;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.beans.LicenseUpdateInfo;
import software.wings.beans.Service;
import software.wings.beans.SubdomainUrl;
import software.wings.beans.TechStack;
import software.wings.features.api.FeatureService;
import software.wings.licensing.LicenseService;
import software.wings.scheduler.ServiceInstanceUsageCheckerJob;
import software.wings.security.UserThreadLocal;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.security.annotations.PublicApi;
import software.wings.service.InstanceSyncController;
import software.wings.service.impl.analysis.CVEnabledService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.UserService;
import software.wings.utils.AccountPermissionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api("account")
@Path("/account")
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class AccountResource {
  @Inject private AccountService accountService;
  @Inject private UserService userService;
  @Inject private LicenseService licenseService;
  @Inject private AccountPermissionUtils accountPermissionUtils;
  @Inject private FeatureService featureService;
  @Inject @Named("BackgroundJobScheduler") private transient PersistentScheduler jobScheduler;
  @Inject private GcpMarketPlaceApiHandler gcpMarketPlaceApiHandler;
  @Inject InstanceSyncController instanceSyncController;

  @GET
  @Path("{accountId}/status")
  @Timed
  @ExceptionMetered
  @PublicApi
  public RestResponse<String> getStatus(@PathParam("accountId") String accountId) {
    return new RestResponse<>(accountService.getAccountStatus(accountId));
  }

  @POST
  @Path("disable")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> disableAccount(
      @QueryParam("accountId") String accountId, @QueryParam("migratedTo") String migratedToClusterUrl) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Disabling account");
      RestResponse<Boolean> response = accountPermissionUtils.checkIfHarnessUser("User not allowed to disable account");
      if (response == null) {
        response = new RestResponse<>(accountService.disableAccount(accountId, urlDecode(migratedToClusterUrl)));
      }
      return response;
    }
  }

  @POST
  @Path("enable")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> enableAccount(@QueryParam("accountId") String accountId) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Enabling account");
      RestResponse<Boolean> response = accountPermissionUtils.checkIfHarnessUser("User not allowed to enable account");
      if (response == null) {
        response = new RestResponse<>(accountService.enableAccount(accountId));
      }
      return response;
    }
  }

  @POST
  @Path("cloudcost/enable")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> enableCloudCost(@QueryParam("accountId") String accountId) {
    RestResponse<Boolean> response =
        accountPermissionUtils.checkIfHarnessUser("User is not allowed to enable cloud cost for the account.");
    if (response == null) {
      response = new RestResponse<>(accountService.updateCloudCostEnabled(accountId, true));
    }
    return response;
  }

  @POST
  @Path("cloudcost/disable")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> disableCloudCost(@QueryParam("accountId") String accountId) {
    RestResponse<Boolean> response =
        accountPermissionUtils.checkIfHarnessUser("User not allowed to disable cloud cost for the account.");
    if (response == null) {
      response = new RestResponse<>(accountService.updateCloudCostEnabled(accountId, false));
    }
    return response;
  }

  @POST
  @Path("continuous-efficiency/enableCeK8sEventCollection")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> enableK8sEventCollection(@QueryParam("accountId") String accountId) {
    RestResponse<Boolean> response =
        accountPermissionUtils.checkIfHarnessUser("User is not allowed to enable cloud cost for the account.");
    if (response == null) {
      response = new RestResponse<>(accountService.updateCeAutoCollectK8sEvents(accountId, true));
    }
    return response;
  }

  @POST
  @Path("continuous-efficiency/disableCeK8sEventCollection")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> disableK8sEventCollection(@QueryParam("accountId") String accountId) {
    RestResponse<Boolean> response =
        accountPermissionUtils.checkIfHarnessUser("User is not allowed to enable cloud cost for the account.");
    if (response == null) {
      response = new RestResponse<>(accountService.updateCeAutoCollectK8sEvents(accountId, false));
    }
    return response;
  }

  @GET
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<PageResponse<Account>> getAccounts(@QueryParam("offset") String offset) {
    PageRequest<Account> accountPageRequest =
        aPageRequest().withOffset(offset).withLimit(String.valueOf(PageRequest.DEFAULT_PAGE_SIZE)).build();
    return new RestResponse<>(accountService.getAccounts(accountPageRequest));
  }

  @GET
  @Path("feature-flag-enabled")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<Boolean> isFeatureEnabled(
      @QueryParam("featureName") String featureName, @QueryParam("accountId") String accountId) {
    return new RestResponse<>(accountService.isFeatureFlagEnabled(featureName, accountId));
  }

  @GET
  @Path("services-cv-24x7")
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<CVEnabledService>> getAllServicesFor24x7(@QueryParam("accountId") String accountId,
      @QueryParam("serviceId") String serviceId, @BeanParam PageRequest<String> request) {
    return new RestResponse<>(
        accountService.getServices(accountId, UserThreadLocal.get().getPublicUser(), request, serviceId));
  }

  @GET
  @Path("services-cv-24x7-breadcrumb")
  @Timed
  @ExceptionMetered
  public RestResponse<List<Service>> getAllServicesFor24x7(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(accountService.getServicesBreadCrumb(accountId, UserThreadLocal.get().getPublicUser()));
  }

  @PUT
  @Path("license")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> updateAccountLicense(
      @QueryParam("accountId") @NotEmpty String accountId, @NotNull LicenseUpdateInfo licenseUpdateInfo) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Updating account license");
      String fromAccountType = accountService.getAccountType(accountId).orElse(AccountType.PAID);
      String toAccountType = licenseUpdateInfo.getLicenseInfo().getAccountType();

      AccountMigration migration = null;
      if (!fromAccountType.equals(toAccountType)) {
        migration = AccountMigration.from(fromAccountType, toAccountType)
                        .orElseThrow(() -> new InvalidRequestException("Unsupported migration", WingsException.USER));
      }

      RestResponse<Boolean> response =
          accountPermissionUtils.checkIfHarnessUser("User not allowed to update account license");
      if (response == null
          || (migration != null && migration.isSelfService() && userService.isAccountAdmin(accountId))) {
        Map<String, Map<String, Object>> requiredInfoToComply = licenseUpdateInfo.getRequiredInfoToComply() == null
            ? Collections.emptyMap()
            : licenseUpdateInfo.getRequiredInfoToComply();

        if (migration != null) {
          if (!featureService.complyFeatureUsagesWithRestrictions(accountId, toAccountType, requiredInfoToComply)) {
            throw new WingsException("Can not update account license. Account is using restricted features");
          }
        }
        boolean licenseUpdated = licenseService.updateAccountLicense(accountId, licenseUpdateInfo.getLicenseInfo());
        if (migration != null) {
          featureService.complyFeatureUsagesWithRestrictions(accountId, requiredInfoToComply);
          // Special Case. Enforce Service Instances Limits only for COMMUNITY account
          if (toAccountType.equals(AccountType.COMMUNITY)) {
            ServiceInstanceUsageCheckerJob.add(jobScheduler, accountId);
          }
        }

        if (licenseUpdated) {
          logger.info("license updated for account {}, license type is {} and status is {}", accountId,
              licenseUpdateInfo.getLicenseInfo().getAccountType(),
              licenseUpdateInfo.getLicenseInfo().getAccountStatus());
        }
        response = new RestResponse<>(licenseUpdated);
      }

      return response;
    }
  }

  @PUT
  @Path("license/{accountId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> updateAccountLicense(
      @PathParam("accountId") @NotEmpty String accountId, @NotNull LicenseInfo licenseInfo) {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Updating account license", accountId);
      return updateAccountLicense(accountId, LicenseUpdateInfo.builder().licenseInfo(licenseInfo).build());
    }
  }

  @PUT
  @Path("{accountId}/tech-stacks")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> updateTechStacks(
      @PathParam("accountId") @NotEmpty String accountId, Set<TechStack> techStacks) {
    return new RestResponse<>(accountService.updateTechStacks(accountId, techStacks));
  }

  @PUT
  @Path("{accountId}/sales-contacts")
  @Timed
  @ExceptionMetered
  public RestResponse<Account> updateAccountSalesContacts(
      @PathParam("accountId") @NotEmpty String accountId, AccountSalesContactsInfo salesContactsInfo) {
    RestResponse<Account> response =
        accountPermissionUtils.checkIfHarnessUser("User not allowed to update account sales contacts");
    if (response == null) {
      response = new RestResponse<>(
          licenseService.updateAccountSalesContacts(accountId, salesContactsInfo.getSalesContacts()));
    }
    return response;
  }

  @PUT
  @Path("license/generate/{accountId}")
  @Timed
  @ExceptionMetered
  public RestResponse<String> generateLicense(
      @PathParam("accountId") @NotEmpty String accountId, LicenseInfo licenseInfo) {
    RestResponse<String> response =
        accountPermissionUtils.checkIfHarnessUser("User not allowed to generate a new license");
    if (response == null) {
      response = new RestResponse<>(licenseService.generateLicense(licenseInfo));
    }
    return response;
  }

  @GET
  @Path("delegate/active")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> checkSampleDelegate(@QueryParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(accountService.sampleDelegateExists(accountId));
  }

  @GET
  @Path("delegate/progress")
  @Timed
  @ExceptionMetered
  public RestResponse<List<ProvisionStep>> checkProgressSampleDelegate(
      @QueryParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(accountService.sampleDelegateProgress(accountId));
  }

  @POST
  @Path("delegate/generate")
  @Timed
  @ExceptionMetered
  public RestResponse<String> generateSampleDelegate(@QueryParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(accountService.generateSampleDelegate(accountId));
  }

  @DELETE
  @Path("delete/{accountId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> deleteAccount(@PathParam("accountId") @NotEmpty String accountId) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Deleting account");
      RestResponse<Boolean> response = accountPermissionUtils.checkIfHarnessUser("User not allowed to delete account");
      if (response == null) {
        response = new RestResponse<>(accountService.delete(accountId));
      }
      return response;
    }
  }

  @DELETE
  @Path("export-delete")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> deleteExportableAccountData(@QueryParam("accountId") @NotEmpty String accountId) {
    RestResponse<Boolean> response = accountPermissionUtils.checkIfHarnessUser("User not allowed to delete account");
    if (response == null) {
      response = new RestResponse<>(accountService.deleteExportableAccountData(accountId));
    }
    return response;
  }

  @POST
  @Path("new")
  @Timed
  @ExceptionMetered
  public RestResponse<Account> createAccount(@NotNull Account account) {
    RestResponse<Account> response = accountPermissionUtils.checkIfHarnessUser("User not allowed to create account");
    if (response == null) {
      account.setAppId(GLOBAL_APP_ID);
      response = new RestResponse<>(accountService.save(account, false));
    }
    return response;
  }

  @GET
  @Path("{accountId}")
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<Account> getAccount(@PathParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(accountService.get(accountId));
  }

  // Fetches account info from DB & not from local manager cache to avoid inconsistencies in UI when account is updated
  @GET
  @Path("{accountId}/latest")
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<Account> getLatestAccount(@PathParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(accountService.get(accountId));
  }

  @GET
  @Path("{accountId}/whitelisted-domains")
  public RestResponse<Set<String>> getWhitelistedDomains(@PathParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(accountService.getWhitelistedDomains(accountId));
  }

  @PUT
  @Path("{accountId}/whitelisted-domains")
  @AuthRule(permissionType = ACCOUNT_MANAGEMENT)
  public RestResponse<Account> updateWhitelistedDomains(
      @PathParam("accountId") @NotEmpty String accountId, @Body Set<String> whitelistedDomains) {
    return new RestResponse<>(accountService.updateWhitelistedDomains(accountId, whitelistedDomains));
  }

  @POST
  @Path("/gcp")
  @Produces(MediaType.TEXT_HTML)
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @PublicApi
  public Response gcpSignUp(@FormParam(value = "x-gcp-marketplace-token") String token,
      @Context HttpServletRequest request, @Context HttpServletResponse response) {
    return gcpMarketPlaceApiHandler.signUp(token);
  }

  @POST
  @Path("custom-event")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<Boolean> postCustomEvent(@QueryParam("accountId") @NotEmpty String accountId,
      @NotNull AccountEvent accountEvent, @QueryParam("oneTimeOnly") @DefaultValue("true") boolean oneTimeOnly,
      @QueryParam("trialOnly") @DefaultValue("true") boolean trialOnly) {
    return new RestResponse<>(accountService.postCustomEvent(accountId, accountEvent, oneTimeOnly, trialOnly));
  }

  @PATCH
  @Path("{accountId}/addSubdomainUrl")
  public RestResponse<Boolean> addSubdomainUrl(
      @PathParam("accountId") @NotEmpty String accountId, @NotNull SubdomainUrl subdomainUrl) {
    String userId = UserThreadLocal.get().getUuid();
    return new RestResponse<>(accountService.addSubdomainUrl(userId, accountId, subdomainUrl));
  }
  @POST
  @Path("{accountId}/enable-perpetual-task")
  public RestResponse<Boolean> enablePerpetualTask(@PathParam("accountId") @NotEmpty String accountId,
      @QueryParam("inframappingType") @NotEmpty String inframappingType) {
    RestResponse<Boolean> response =
        accountPermissionUtils.checkIfHarnessUser("User not allowed to disable cloud cost for the account.");
    if (response != null) {
      throw new UnauthorizedException("User not authorized", WingsException.USER);
    }
    return new RestResponse<>(instanceSyncController.enablePerpetualTaskForAccount(accountId, inframappingType));
  }
}

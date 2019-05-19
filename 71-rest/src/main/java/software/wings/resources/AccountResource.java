package software.wings.resources;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.FeatureViolation.Category.RESTRICTED_FEATURE_USAGE;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.hazelcast.util.CollectionUtil;
import io.harness.account.ProvisionStep;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.InvalidRequestException;
import io.harness.rest.RestResponse;
import io.harness.rest.RestResponse.Builder;
import io.harness.scheduler.PersistentScheduler;
import io.swagger.annotations.Api;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Account;
import software.wings.beans.AccountSalesContactsInfo;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.FeatureViolation;
import software.wings.beans.LicenseInfo;
import software.wings.beans.Service;
import software.wings.beans.User;
import software.wings.beans.security.UserGroup;
import software.wings.licensing.LicenseService;
import software.wings.licensing.violations.FeatureViolationsService;
import software.wings.licensing.violations.RestrictedFeature;
import software.wings.scheduler.DelegateDeletionJobForCommunityAccount;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.UserThreadLocal;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.security.annotations.PublicApi;
import software.wings.security.authentication.TOTPAuthHandler;
import software.wings.service.impl.analysis.CVEnabledService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.HarnessUserGroupService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.compliance.GovernanceConfigService;
import software.wings.utils.AccountPermissionUtils;
import software.wings.utils.CacheHelper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
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

@Api("account")
@Path("/account")
@Produces(MediaType.APPLICATION_JSON)
public class AccountResource {
  @Inject private AccountService accountService;
  @Inject private LicenseService licenseService;
  @Inject private AccountPermissionUtils accountPermissionUtils;
  @Inject private CacheHelper cacheHelper;
  @Inject private GovernanceConfigService governanceConfigService;
  @Inject private UserService userService;
  @Inject private UserGroupService userGroupService;
  @Inject private TOTPAuthHandler totpHandler;
  @Inject private HarnessUserGroupService harnessUserGroupService;
  @Inject private FeatureViolationsService featureViolationsService;
  @Inject private DelegateService delegateService;
  @Inject @Named("BackgroundJobScheduler") private transient PersistentScheduler jobScheduler;

  @GET
  @Path("{accountId}/status")
  @Timed
  @ExceptionMetered
  @PublicApi
  public RestResponse<String> getStatus(@PathParam("accountId") String accountId) {
    return new RestResponse<>(accountService.getAccountStatus(accountId));
  }

  @POST
  @Path("{accountId}/start-migration")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> startMigration(@PathParam("accountId") String accountId) {
    RestResponse<Boolean> response =
        accountPermissionUtils.checkIfHarnessUser("User not allowed to start account migration");
    if (response == null) {
      response = new RestResponse<>(accountService.startAccountMigration(accountId));
    }
    return response;
  }

  @POST
  @Path("{accountId}/complete-migration")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> completeMigration(@PathParam("accountId") String accountId) {
    RestResponse<Boolean> response =
        accountPermissionUtils.checkIfHarnessUser("User not allowed to complete account migration");
    if (response == null) {
      response = new RestResponse<>(accountService.completeAccountMigration(accountId));
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
  @LearningEngineAuth
  public RestResponse<PageResponse<CVEnabledService>> getAllServicesFor24x7(@QueryParam("accountId") String accountId,
      @QueryParam("serviceId") String serviceId, @BeanParam PageRequest<String> request) {
    return new RestResponse<>(
        accountService.getServices(accountId, UserThreadLocal.get().getPublicUser(), request, serviceId));
  }

  @GET
  @Path("services-cv-24x7-breadcrumb")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<List<Service>> getAllServicesFor24x7(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(accountService.getServicesBreadCrumb(accountId, UserThreadLocal.get().getPublicUser()));
  }

  @PUT
  @Path("license/{accountId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> updateAccountLicense(
      @PathParam("accountId") @NotEmpty String accountId, LicenseInfo licenseInfo) {
    RestResponse<Boolean> response =
        accountPermissionUtils.checkIfHarnessUser("User not allowed to update account license");
    if (response == null) {
      response = new RestResponse<>(licenseService.updateAccountLicense(accountId, licenseInfo));
    }
    return response;
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
    RestResponse<Boolean> response = accountPermissionUtils.checkIfHarnessUser("User not allowed to delete account");
    if (response == null) {
      response = new RestResponse<>(accountService.delete(accountId));
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
      response = new RestResponse<>(accountService.save(account));
    }
    return response;
  }

  @GET
  @Path("{accountId}")
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public RestResponse<Account> getAccount(@PathParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(accountService.getFromCache(accountId));
  }

  @GET
  @Path("{accountId}/license-violations")
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  @Timed
  public RestResponse<List<FeatureViolation>> getLicenseViolations(@PathParam("accountId") @NotEmpty String accountId,
      @QueryParam("targetAccountType") @NotEmpty String targetAccountType) {
    return new RestResponse<>(featureViolationsService.getViolations(accountId, targetAccountType));
  }

  @GET
  @Path("{accountId}/restricted-features")
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public RestResponse<List<RestrictedFeature>> getRestrictedFeatures(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(featureViolationsService.getRestrictedFeatures(accountId));
  }

  /*
  This API is used by UI to migrate the account to COMMUNITY license.
  This API is a wrapper over account/license/{accountId} API.
  This API enables execution of some actions (like fix account violations) before updating account license to community.
   */
  @PUT
  @Path("transition-to-community/{accountId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> updateAccountLicenseToCommunity(
      @PathParam("accountId") @NotEmpty String accountId, @NotNull Map<String, List<String>> properties) {
    User authenticatedUser = UserThreadLocal.get();
    if (authenticatedUser == null) {
      throw new InvalidRequestException("Invalid User");
    }

    List<FeatureViolation> violationsToBeFixedBeforeAccountTransition =
        featureViolationsService.getViolations(accountId, AccountType.COMMUNITY)
            .stream()
            .filter(violation
                -> violation.getViolationCategory() != RESTRICTED_FEATURE_USAGE
                    && violation.getRestrictedFeature() != RestrictedFeature.DELEGATE)
            .collect(Collectors.toList());

    if (!violationsToBeFixedBeforeAccountTransition.isEmpty()) {
      return Builder.aRestResponse()
          .withResponseMessages(Lists.newArrayList(
              ResponseMessage.builder()
                  .message("Account is using restricted features. Fix the violations before proceeding.")
                  .build()))
          .build();
    }

    Optional<String> currentAccountType = accountService.getAccountType(accountId);
    boolean accountTransitionFromTrial =
        currentAccountType.isPresent() && AccountType.TRIAL.equals(currentAccountType.get());

    boolean isMemberOfAdminUserGroup =
        userGroupService.getUserGroupsByAccountId(accountId, authenticatedUser)
            .stream()
            .anyMatch(userGroup -> userGroup.getName().equals(UserGroup.DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME));

    if (harnessUserGroupService.isHarnessSupportUser(authenticatedUser.getUuid())
        || (accountTransitionFromTrial && isMemberOfAdminUserGroup)) {
      // Assign all users membership of 'Administrator' user group.
      List<User> usersOfAccount = userService.getUsersOfAccount(accountId);
      UserGroup adminUserGroup = userGroupService.getAdminUserGroup(accountId);
      usersOfAccount.forEach(user
          -> userService.updateUserGroupsOfUser(
              user.getUuid(), Collections.singletonList(adminUserGroup), accountId, false));

      // Delete delegates
      List<String> delegatesToRetain = null;
      if (properties.containsKey("delegatesToRetain")) {
        delegatesToRetain = properties.get("delegatesToRetain");
      }

      LicenseInfo communityLicenseInfo =
          LicenseInfo.builder().accountType(AccountType.COMMUNITY).accountStatus(AccountStatus.ACTIVE).build();

      RestResponse<Boolean> result = updateAccountLicense(accountId, communityLicenseInfo);

      boolean licenseUpdated = result.getResource() != null && result.getResource();
      if (licenseUpdated && CollectionUtil.isNotEmpty(delegatesToRetain)) {
        DelegateDeletionJobForCommunityAccount.addWithDelay(jobScheduler, accountId, delegatesToRetain.get(0), 30);
      }

      return result;
    }

    return Builder.aRestResponse()
        .withResponseMessages(
            Lists.newArrayList(ResponseMessage.builder().message("User not allowed to update account license").build()))
        .build();
  }
}

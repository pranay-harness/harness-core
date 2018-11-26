package software.wings.resources;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.exception.InvalidRequestException;
import io.swagger.annotations.Api;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Account;
import software.wings.beans.FeatureName;
import software.wings.beans.LicenseInfo;
import software.wings.beans.ResponseMessage;
import software.wings.beans.RestResponse;
import software.wings.beans.RestResponse.Builder;
import software.wings.beans.Service;
import software.wings.beans.User;
import software.wings.licensing.LicenseService;
import software.wings.security.UserThreadLocal;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.security.annotations.PublicApi;
import software.wings.service.impl.analysis.CVEnabledService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.HarnessUserGroupService;

import java.util.List;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
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
  @Inject private HarnessUserGroupService harnessUserGroupService;

  @GET
  @Path("{accountId}/status")
  @Timed
  @ExceptionMetered
  @PublicApi
  public RestResponse<String> getStatus(@PathParam("accountId") String accountId) {
    return new RestResponse<>(accountService.getAccountStatus(accountId));
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
      @QueryParam("featureName") FeatureName featureName, @QueryParam("accountId") String accountId) {
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
  public RestResponse<Account> updateAccountLicense(@PathParam("accountId") @NotEmpty String accountId,
      @QueryParam("salesContacts") String salesContacts, LicenseInfo licenseInfo) {
    User existingUser = UserThreadLocal.get();
    if (existingUser == null) {
      throw new InvalidRequestException("Invalid User");
    }

    if (harnessUserGroupService.isHarnessSupportUser(existingUser.getUuid())) {
      return new RestResponse<>(licenseService.updateAccountLicense(accountId, licenseInfo, salesContacts));
    } else {
      return Builder.aRestResponse()
          .withResponseMessages(Lists.newArrayList(
              ResponseMessage.builder().message("User not allowed to update account license").build()))
          .build();
    }
  }

  @PUT
  @Path("license/generate/{accountId}")
  @Timed
  @ExceptionMetered
  public RestResponse<String> generateLicense(
      @PathParam("accountId") @NotEmpty String accountId, LicenseInfo licenseInfo) {
    User existingUser = UserThreadLocal.get();
    if (existingUser == null) {
      throw new InvalidRequestException("Invalid User");
    }

    if (harnessUserGroupService.isHarnessSupportUser(existingUser.getUuid())) {
      return new RestResponse<>(licenseService.generateLicense(licenseInfo));
    } else {
      return Builder.aRestResponse()
          .withResponseMessages(Lists.newArrayList(
              ResponseMessage.builder().message("User not allowed to generate a new license").build()))
          .build();
    }
  }
}

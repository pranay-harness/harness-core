package software.wings.resources;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static java.util.stream.Collectors.toList;
import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT_MANAGEMENT;
import static software.wings.security.PermissionAttribute.ResourceType.DELEGATE;
import static software.wings.service.impl.DelegateServiceImpl.DELEGATE_DIR;
import static software.wings.service.impl.DelegateServiceImpl.DOCKER_DELEGATE;
import static software.wings.service.impl.DelegateServiceImpl.ECS_DELEGATE;
import static software.wings.service.impl.DelegateServiceImpl.HARNESS_DELEGATE_VALUES_YAML;
import static software.wings.service.impl.DelegateServiceImpl.KUBERNETES_DELEGATE;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import freemarker.template.TemplateException;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.data.validator.Trimmed;
import io.harness.delegate.beans.DelegateApproval;
import io.harness.delegate.task.DelegateLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateStatus;
import software.wings.helpers.ext.url.SubdomainUrlHelperIntfc;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.security.annotations.PublicApi;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.DelegateScopeService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.DownloadTokenService;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * Created by rohitkarelia on 11/18/19.
 */
@Api("/setup/delegates")
@Path("/setup/delegates")
@Produces("application/json")
@Scope(DELEGATE)
@Slf4j
public class DelegateSetupResource {
  private static final String DOWNLOAD_URL = "downloadUrl";
  private static final String ACCOUNT_ID = "?accountId=";
  private static final String TOKEN = "&token=";
  private static final String DELEGATE = "delegate.";
  private static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
  private static final String BINARY = "binary";
  private static final String APPLICATION_ZIP_CHARSET_BINARY = "application/zip; charset=binary";
  private static final String CONTENT_DISPOSITION = "Content-Disposition";
  private static final String ATTACHMENT_FILENAME = "attachment; filename=";
  private static final String TAR_GZ = ".tar.gz";

  private DelegateService delegateService;
  private DelegateScopeService delegateScopeService;
  private DownloadTokenService downloadTokenService;
  private SubdomainUrlHelperIntfc subdomainUrlHelper;

  @Inject
  public DelegateSetupResource(DelegateService delegateService, DelegateScopeService delegateScopeService,
      DownloadTokenService downloadTokenService, SubdomainUrlHelperIntfc subdomainUrlHelper) {
    this.delegateService = delegateService;
    this.delegateScopeService = delegateScopeService;
    this.downloadTokenService = downloadTokenService;
    this.subdomainUrlHelper = subdomainUrlHelper;
  }

  @GET
  @ApiImplicitParams(
      { @ApiImplicitParam(name = "accountId", required = true, dataType = "string", paramType = "query") })
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Delegate>>
  list(@BeanParam PageRequest<Delegate> pageRequest) {
    return new RestResponse<>(delegateService.list(pageRequest));
  }

  @GET
  @Path("status")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateStatus> listDelegateStatus(@QueryParam("accountId") @NotEmpty String accountId) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      return new RestResponse<>(delegateService.getDelegateStatus(accountId));
    }
  }

  @GET
  @Path("available-versions-for-verification")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<List<String>> getAvailableVersions(@QueryParam("accountId") @NotEmpty String accountId) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      return new RestResponse<>(delegateService.getAvailableVersions(accountId));
    }
  }

  @GET
  @Path("latest")
  @Timed
  @ExceptionMetered
  public RestResponse<String> get(@QueryParam("accountId") @NotEmpty String accountId) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      return new RestResponse<>(delegateService.getLatestDelegateVersion(accountId));
    }
  }

  @GET
  @Path("{delegateId}/profile-result")
  @Timed
  @ExceptionMetered
  public RestResponse<String> getProfileResult(
      @PathParam("delegateId") String delegateId, @QueryParam("accountId") @NotEmpty String accountId) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      return new RestResponse<>(delegateService.getProfileResult(accountId, delegateId));
    }
  }

  @PUT
  @Path("{delegateId}/scopes")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ACCOUNT_MANAGEMENT)
  public RestResponse<Delegate> updateScopes(@PathParam("delegateId") @NotEmpty String delegateId,
      @QueryParam("accountId") @NotEmpty String accountId, DelegateScopes delegateScopes) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      Delegate delegate = delegateService.get(accountId, delegateId, true);
      if (delegateScopes == null) {
        delegate.setIncludeScopes(null);
        delegate.setExcludeScopes(null);
      } else {
        if (isNotEmpty(delegateScopes.getIncludeScopeIds())) {
          delegate.setIncludeScopes(delegateScopes.getIncludeScopeIds()
                                        .stream()
                                        .map(s -> delegateScopeService.get(accountId, s))
                                        .filter(Objects::nonNull)
                                        .collect(toList()));
        } else {
          delegate.setIncludeScopes(null);
        }
        if (isNotEmpty(delegateScopes.getExcludeScopeIds())) {
          delegate.setExcludeScopes(delegateScopes.getExcludeScopeIds()
                                        .stream()
                                        .map(s -> delegateScopeService.get(accountId, s))
                                        .filter(Objects::nonNull)
                                        .collect(toList()));
        } else {
          delegate.setExcludeScopes(null);
        }
      }
      return new RestResponse<>(delegateService.updateScopes(delegate));
    }
  }

  @VisibleForTesting
  protected static class DelegateScopes {
    private List<String> includeScopeIds;
    private List<String> excludeScopeIds;

    public List<String> getIncludeScopeIds() {
      return includeScopeIds;
    }

    public void setIncludeScopeIds(List<String> includeScopeIds) {
      this.includeScopeIds = includeScopeIds;
    }

    public List<String> getExcludeScopeIds() {
      return excludeScopeIds;
    }

    public void setExcludeScopeIds(List<String> excludeScopeIds) {
      this.excludeScopeIds = excludeScopeIds;
    }
  }

  @PUT
  @Path("{delegateId}/tags")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ACCOUNT_MANAGEMENT)
  public RestResponse<Delegate> updateTags(@PathParam("delegateId") @NotEmpty String delegateId,
      @QueryParam("accountId") @NotEmpty String accountId, DelegateTags delegateTags) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      Delegate delegate = delegateService.get(accountId, delegateId, true);
      delegate.setTags(delegateTags.getTags());
      return new RestResponse<>(delegateService.updateTags(delegate));
    }
  }

  @VisibleForTesting
  protected static class DelegateTags {
    private List<String> tags;
    public List<String> getTags() {
      return tags;
    }
    public void setTags(List<String> tags) {
      this.tags = tags;
    }
  }

  @GET
  @Path("kubernetes-delegates")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> kubernetesDelegateNames(
      @Context HttpServletRequest request, @QueryParam("accountId") @NotEmpty String accountId) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      return new RestResponse<>(delegateService.getKubernetesDelegateNames(accountId));
    }
  }

  /*
    Duplicated method in case of Rollback, soon as "delegate-selectors" is verified and tested from UI,
    below method "delegate-tags" will be removed.
  */
  @GET
  @Path("delegate-selectors")
  @Timed
  @ExceptionMetered
  public RestResponse<Set<String>> delegateSelectors(
      @Context HttpServletRequest request, @QueryParam("accountId") @NotEmpty String accountId) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      return new RestResponse<>(delegateService.getAllDelegateSelectors(accountId));
    }
  }

  @GET
  @Path("delegate-tags")
  @Timed
  @ExceptionMetered
  public RestResponse<Set<String>> delegateTags(
      @Context HttpServletRequest request, @QueryParam("accountId") @NotEmpty String accountId) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      return new RestResponse<>(delegateService.getAllDelegateSelectors(accountId));
    }
  }

  @GET
  @Path("{delegateId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Delegate> get(
      @PathParam("delegateId") @NotEmpty String delegateId, @QueryParam("accountId") @NotEmpty String accountId) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      return new RestResponse<>(delegateService.get(accountId, delegateId, true));
    }
  }

  @PUT
  @Path("{delegateId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ACCOUNT_MANAGEMENT)
  public RestResponse<Delegate> update(@PathParam("delegateId") @NotEmpty String delegateId,
      @QueryParam("accountId") @NotEmpty String accountId, Delegate delegate) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      delegate.setAccountId(accountId);
      delegate.setUuid(delegateId);

      Delegate existingDelegate = delegateService.get(accountId, delegateId, true);
      if (existingDelegate != null) {
        delegate.setDelegateType(existingDelegate.getDelegateType());
        delegate.setDelegateGroupName(existingDelegate.getDelegateGroupName());
      }

      return new RestResponse<>(delegateService.update(delegate));
    }
  }

  @PUT
  @Path("{delegateId}/description")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ACCOUNT_MANAGEMENT)
  public RestResponse<Delegate> updateDescription(@PathParam("delegateId") @NotEmpty String delegateId,
      @QueryParam("accountId") @NotEmpty String accountId, @Trimmed String newDescription) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      return new RestResponse<>(delegateService.updateDescription(accountId, delegateId, newDescription));
    }
  }

  @PUT
  @Path("{delegateId}/approval")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ACCOUNT_MANAGEMENT)
  public RestResponse<Delegate> updateApprovalStatus(@PathParam("delegateId") @NotEmpty String delegateId,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("action") @NotNull DelegateApproval action) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      return new RestResponse<>(delegateService.updateApprovalStatus(accountId, delegateId, action));
    }
  }

  @DELETE
  @Path("{delegateId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ACCOUNT_MANAGEMENT)
  public RestResponse<Void> delete(
      @PathParam("delegateId") @NotEmpty String delegateId, @QueryParam("accountId") @NotEmpty String accountId) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      delegateService.delete(accountId, delegateId);
      return new RestResponse<>();
    }
  }
  @DELETE
  @Path("delete-all-except")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ACCOUNT_MANAGEMENT)
  public RestResponse<Void> deleteAllExcept(
      @QueryParam("accountId") @NotEmpty String accountId, List<String> delegatesToRetain) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      delegateService.retainOnlySelectedDelegatesAndDeleteRest(accountId, delegatesToRetain);
      return new RestResponse<>();
    }
  }

  @GET
  @Path(DOWNLOAD_URL)
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, String>> downloadUrl(
      @Context HttpServletRequest request, @QueryParam("accountId") @NotEmpty String accountId) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      String url = subdomainUrlHelper.getManagerUrl(request, accountId);

      return new RestResponse<>(ImmutableMap.of(DOWNLOAD_URL,
          url + request.getRequestURI().replace(DOWNLOAD_URL, "download") + ACCOUNT_ID + accountId + TOKEN
              + downloadTokenService.createDownloadToken(DELEGATE + accountId),
          "dockerUrl",
          url + request.getRequestURI().replace(DOWNLOAD_URL, "docker") + ACCOUNT_ID + accountId + TOKEN
              + downloadTokenService.createDownloadToken(DELEGATE + accountId),
          "kubernetesUrl",
          url + request.getRequestURI().replace(DOWNLOAD_URL, "kubernetes") + ACCOUNT_ID + accountId + TOKEN
              + downloadTokenService.createDownloadToken(DELEGATE + accountId),
          "ecsUrl",
          url + request.getRequestURI().replace(DOWNLOAD_URL, "ecs") + ACCOUNT_ID + accountId + TOKEN
              + downloadTokenService.createDownloadToken(DELEGATE + accountId)));
    }
  }

  @PublicApi
  @GET
  @Path("download")
  @Timed
  @ExceptionMetered
  public Response downloadScripts(@Context HttpServletRequest request,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("token") @NotEmpty String token)
      throws IOException, TemplateException {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      downloadTokenService.validateDownloadToken(DELEGATE + accountId, token);
      File delegateFile = delegateService.downloadScripts(
          subdomainUrlHelper.getManagerUrl(request, accountId), getVerificationUrl(request), accountId);
      return Response.ok(delegateFile)
          .header(CONTENT_TRANSFER_ENCODING, BINARY)
          .type(APPLICATION_ZIP_CHARSET_BINARY)
          .header(CONTENT_DISPOSITION, ATTACHMENT_FILENAME + DELEGATE_DIR + TAR_GZ)
          .build();
    }
  }

  @PublicApi
  @GET
  @Path("docker")
  @Timed
  @ExceptionMetered
  public Response downloadDocker(@Context HttpServletRequest request,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("delegateName") String delegateName,
      @QueryParam("delegateProfileId") String delegateProfileId, @QueryParam("token") @NotEmpty String token)
      throws IOException, TemplateException {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      downloadTokenService.validateDownloadToken(DELEGATE + accountId, token);
      File delegateFile = delegateService.downloadDocker(subdomainUrlHelper.getManagerUrl(request, accountId),
          getVerificationUrl(request), accountId, delegateName, delegateProfileId);
      return Response.ok(delegateFile)
          .header(CONTENT_TRANSFER_ENCODING, BINARY)
          .type(APPLICATION_ZIP_CHARSET_BINARY)
          .header(CONTENT_DISPOSITION, ATTACHMENT_FILENAME + DOCKER_DELEGATE + TAR_GZ)
          .build();
    }
  }

  @PublicApi
  @GET
  @Path("kubernetes")
  @Timed
  @ExceptionMetered
  public Response downloadKubernetes(@Context HttpServletRequest request,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("delegateName") @NotEmpty String delegateName,
      @QueryParam("delegateProfileId") String delegateProfileId, @QueryParam("token") @NotEmpty String token)
      throws IOException, TemplateException {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      downloadTokenService.validateDownloadToken(DELEGATE + accountId, token);
      File delegateFile = delegateService.downloadKubernetes(subdomainUrlHelper.getManagerUrl(request, accountId),
          getVerificationUrl(request), accountId, delegateName, delegateProfileId);
      return Response.ok(delegateFile)
          .header(CONTENT_TRANSFER_ENCODING, BINARY)
          .type(APPLICATION_ZIP_CHARSET_BINARY)
          .header(CONTENT_DISPOSITION, ATTACHMENT_FILENAME + KUBERNETES_DELEGATE + TAR_GZ)
          .build();
    }
  }

  @PublicApi
  @GET
  @Path("ecs")
  @Timed
  @ExceptionMetered
  public Response downloadEcs(@Context HttpServletRequest request, @QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("delegateGroupName") @NotEmpty String delegateGroupName, @QueryParam("awsVpcMode") Boolean awsVpcMode,
      @QueryParam("hostname") String hostname, @QueryParam("delegateProfileId") String delegateProfileId,
      @QueryParam("token") @NotEmpty String token) throws IOException, TemplateException {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      downloadTokenService.validateDownloadToken(DELEGATE + accountId, token);
      File delegateFile = delegateService.downloadECSDelegate(subdomainUrlHelper.getManagerUrl(request, accountId),
          getVerificationUrl(request), accountId, awsVpcMode, hostname, delegateGroupName, delegateProfileId);
      return Response.ok(delegateFile)
          .header(CONTENT_TRANSFER_ENCODING, BINARY)
          .type(APPLICATION_ZIP_CHARSET_BINARY)
          .header(CONTENT_DISPOSITION, ATTACHMENT_FILENAME + ECS_DELEGATE + TAR_GZ)
          .build();
    }
  }

  @PublicApi
  @GET
  @Path("delegate-helm-values-yaml")
  @Timed
  @ExceptionMetered
  public Response downloadDelegateValuesYaml(@Context HttpServletRequest request,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("delegateName") @NotEmpty String delegateName,
      @QueryParam("delegateProfileId") String delegateProfileId, @QueryParam("token") @NotEmpty String token)
      throws IOException, TemplateException {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      downloadTokenService.validateDownloadToken(DELEGATE + accountId, token);
      File delegateFile =
          delegateService.downloadDelegateValuesYamlFile(subdomainUrlHelper.getManagerUrl(request, accountId),
              getVerificationUrl(request), accountId, delegateName, delegateProfileId);
      return Response.ok(delegateFile)
          .header(CONTENT_TRANSFER_ENCODING, BINARY)
          .type("text/plain; charset=UTF-8")
          .header(CONTENT_DISPOSITION, ATTACHMENT_FILENAME + HARNESS_DELEGATE_VALUES_YAML + ".yaml")
          .build();
    }
  }

  private String getVerificationUrl(HttpServletRequest request) {
    return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
  }
}

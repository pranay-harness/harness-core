package software.wings.resources;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static software.wings.security.PermissionAttribute.ResourceType.DELEGATE;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateHeartbeatResponse;
import io.harness.delegate.beans.*;
import io.harness.delegate.task.DelegateLogContext;
import io.harness.delegate.task.TaskLogContext;
import io.harness.delegate.task.validation.DelegateConnectionResultDetail;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.managerclient.*;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.DelegateAuth;

import software.wings.beans.Account;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.ratelimit.DelegateRequestRateLimiter;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.DelegateTaskServiceClassic;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import io.swagger.annotations.Api;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;
import org.jetbrains.annotations.NotNull;

@OwnedBy(HarnessTeam.DEL)
@Api("/agent/dms")
@Path("/agent/dms")
@Produces("application/json")
@Scope(DELEGATE)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class DelegateAgentDmsResource {
  private final DelegateService delegateService;
  private final AccountService accountService;
  private final DelegateRequestRateLimiter delegateRequestRateLimiter;
  private final DelegateTaskServiceClassic delegateTaskServiceClassic;

  @DelegateAuth
  @POST
  @Path("delegates/register")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateRegisterResponse> register(
      @QueryParam("accountId") @NotEmpty String accountId, DelegateParams delegateParams) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      long startTime = System.currentTimeMillis();
      DelegateRegisterResponse registerResponse =
          delegateService.register(delegateParams.toBuilder().accountId(accountId).build());
      log.info("Delegate registration took {} in ms", System.currentTimeMillis() - startTime);
      return new RestResponse<>(registerResponse);
    }
  }

  @DelegateAuth
  @POST
  @Path("delegates/connectionHeartbeat/{delegateId}")
  @Timed
  @ExceptionMetered
  public void connectionHeartbeat(@QueryParam("accountId") @NotEmpty String accountId,
      @PathParam("delegateId") String delegateId, DelegateConnectionHeartbeat connectionHeartbeat) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      delegateService.registerHeartbeat(accountId, delegateId, connectionHeartbeat, ConnectionMode.POLLING);
    }
  }

  @DelegateAuth
  @GET
  @Path("delegates/{delegateId}/profile")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateProfileParams> checkForProfile(@QueryParam("accountId") @NotEmpty String accountId,
      @PathParam("delegateId") String delegateId, @QueryParam("profileId") String profileId,
      @QueryParam("lastUpdatedAt") Long lastUpdatedAt) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      DelegateProfileParams profileParams =
          delegateService.checkForProfile(accountId, delegateId, profileId, lastUpdatedAt);
      return new RestResponse<>(profileParams);
    }
  }

  @DelegateAuth
  @POST
  @Path("delegateFiles/{delegateId}/profile-result")
  @Timed
  @ExceptionMetered
  public void saveProfileResult(@PathParam("delegateId") String delegateId,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("error") boolean error,
      @QueryParam("fileBucket") FileBucket fileBucket, @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    delegateService.saveProfileResult(accountId, delegateId, error, fileBucket, uploadedInputStream, fileDetail);
  }

  @DelegateAuth
  @POST
  @Path("delegates/heartbeat-with-polling")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateHeartbeatResponse> updateDelegateHB(
      @QueryParam("accountId") @NotEmpty String accountId, DelegateParams delegateParams) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(delegateParams.getDelegateId(), OVERRIDE_ERROR)) {
      // delegate.isPollingModeEnabled() will be true here.
      if ("ECS".equals(delegateParams.getDelegateType())) {
        Delegate registeredDelegate = delegateService.handleEcsDelegateRequest(buildDelegateFromParams(delegateParams));

        return new RestResponse<>(buildDelegateHBResponse(registeredDelegate));
      } else {
        return new RestResponse<>(buildDelegateHBResponse(
            delegateService.updateHeartbeatForDelegateWithPollingEnabled(buildDelegateFromParams(delegateParams))));
      }
    }
  }

  @DelegateAuth
  @POST
  @Path("delegates/properties")
  @Timed
  @ExceptionMetered
  public RestResponse<String> getDelegateProperties(@QueryParam("accountId") String accountId, byte[] request)
      throws InvalidProtocolBufferException {
    GetDelegatePropertiesRequest requestProto = GetDelegatePropertiesRequest.parseFrom(request);

    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      GetDelegatePropertiesResponse response =
          GetDelegatePropertiesResponse.newBuilder()
              .addAllResponseEntry(
                  requestProto.getRequestEntryList()
                      .stream()
                      .map(requestEntry -> {
                        if (requestEntry.is(DelegateVersionsQuery.class)) {
                          return Any.pack(
                              DelegateVersions.newBuilder()
                                  .addAllDelegateVersion(
                                      accountService.getDelegateConfiguration(accountId).getDelegateVersions())
                                  .build());
                        } else if (requestEntry.is(HttpsCertRequirementQuery.class)) {
                          return Any.pack(
                              HttpsCertRequirement.newBuilder()
                                  .setCertRequirement(accountService.getHttpsCertificateRequirement(accountId))
                                  .build());
                        } else if (requestEntry.is(AccountPreferenceQuery.class)) {
                          Account account = accountService.get(accountId);
                          if (account.getAccountPreferences() != null
                              && account.getAccountPreferences().getDelegateSecretsCacheTTLInHours() != null) {
                            return Any.pack(AccountPreference.newBuilder()
                                                .setDelegateSecretsCacheTTLInHours(
                                                    account.getAccountPreferences().getDelegateSecretsCacheTTLInHours())
                                                .build());
                          }
                          return Any.newBuilder().build();
                        } else {
                          return Any.newBuilder().build();
                        }
                      })
                      .collect(toList()))
              .build();
      return new RestResponse<>(response.toString());

    } catch (Exception e) {
      log.error("Encountered an exception while parsing proto", e);
      return null;
    }
  }

  @DelegateAuth
  @PUT
  @Produces("application/x-kryo")
  @Path("delegates/{delegateId}/tasks/{taskId}/acquire")
  @Timed
  @ExceptionMetered
  public DelegateTaskPackage acquireDelegateTask(@PathParam("delegateId") String delegateId,
      @PathParam("taskId") String taskId, @QueryParam("accountId") @NotEmpty String accountId) {
    try (AutoLogContext ignore1 = new TaskLogContext(taskId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore3 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      if (delegateRequestRateLimiter.isOverRateLimit(accountId, delegateId)) {
        return null;
      }
      return delegateTaskServiceClassic.acquireDelegateTask(accountId, delegateId, taskId);
    }
  }

  @DelegateAuth
  @POST
  @Produces("application/x-kryo")
  @Path("delegates/{delegateId}/tasks/{taskId}/report")
  @Timed
  @ExceptionMetered
  public DelegateTaskPackage reportConnectionResults(@PathParam("delegateId") String delegateId,
      @PathParam("taskId") String taskId, @QueryParam("accountId") @NotEmpty String accountId,
      List<DelegateConnectionResultDetail> results) {
    try (AutoLogContext ignore1 = new TaskLogContext(taskId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore3 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      return delegateTaskServiceClassic.reportConnectionResults(
          accountId, delegateId, taskId, getDelegateConnectionResults(results));
    }
  }

  @DelegateAuth
  @GET
  @Produces("application/x-kryo")
  @Path("delegates/{delegateId}/tasks/{taskId}/fail")
  @Timed
  @ExceptionMetered
  public void failIfAllDelegatesFailed(@PathParam("delegateId") final String delegateId,
      @PathParam("taskId") final String taskId, @QueryParam("accountId") @NotEmpty final String accountId,
      @QueryParam("areClientToolsInstalled") final boolean areClientToolsInstalled) {
    try (AutoLogContext ignore1 = new TaskLogContext(taskId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore3 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      delegateTaskServiceClassic.failIfAllDelegatesFailed(accountId, delegateId, taskId, areClientToolsInstalled);
    }
  }

  @NotNull
  private List<DelegateConnectionResult> getDelegateConnectionResults(List<DelegateConnectionResultDetail> results) {
    List<DelegateConnectionResult> delegateConnectionResult = new ArrayList<>();
    for (DelegateConnectionResultDetail source : results) {
      DelegateConnectionResult target = DelegateConnectionResult.builder().build();
      target.setAccountId(source.getAccountId());
      target.setCriteria(source.getCriteria());
      target.setDelegateId(source.getDelegateId());
      target.setDuration(source.getDuration());
      target.setLastUpdatedAt(source.getLastUpdatedAt());
      target.setUuid(source.getUuid());
      target.setValidated(source.isValidated());
      target.setValidUntil(source.getValidUntil());
      delegateConnectionResult.add(target);
    }
    return delegateConnectionResult;
  }

  private DelegateHeartbeatResponse buildDelegateHBResponse(Delegate delegate) {
    return DelegateHeartbeatResponse.builder()
        .delegateId(delegate.getUuid())
        .delegateRandomToken(delegate.getDelegateRandomToken())
        .jreVersion(delegate.getUseJreVersion())
        .sequenceNumber(delegate.getSequenceNum())
        .status(delegate.getStatus().toString())
        .useCdn(delegate.isUseCdn())
        .build();
  }

  private Delegate buildDelegateFromParams(DelegateParams delegateParams) {
    return Delegate.builder()
        .uuid(delegateParams.getDelegateId())
        .accountId(delegateParams.getAccountId())
        .description(delegateParams.getDescription())
        .ip(delegateParams.getIp())
        .hostName(delegateParams.getHostName())
        .delegateGroupName(delegateParams.getDelegateGroupName())
        .delegateName(delegateParams.getDelegateName())
        .delegateProfileId(delegateParams.getDelegateProfileId())
        .lastHeartBeat(delegateParams.getLastHeartBeat())
        .version(delegateParams.getVersion())
        .sequenceNum(delegateParams.getSequenceNum())
        .delegateType(delegateParams.getDelegateType())
        .delegateRandomToken(delegateParams.getDelegateRandomToken())
        .keepAlivePacket(delegateParams.isKeepAlivePacket())
        .polllingModeEnabled(delegateParams.isPollingModeEnabled())
        .ng(delegateParams.isNg())
        .sampleDelegate(delegateParams.isSampleDelegate())
        .currentlyExecutingDelegateTasks(delegateParams.getCurrentlyExecutingDelegateTasks())
        .location(delegateParams.getLocation())
        .build();
  }
}

package software.wings.resources;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static software.wings.security.PermissionAttribute.ResourceType.DELEGATE;

import static java.util.stream.Collectors.toList;

import io.harness.artifact.ArtifactCollectionResponseHandler;
import io.harness.beans.DelegateHeartbeatResponse;
import io.harness.delegate.beans.ConnectionMode;
import io.harness.delegate.beans.DelegateConfiguration;
import io.harness.delegate.beans.DelegateConnectionHeartbeat;
import io.harness.delegate.beans.DelegateParams;
import io.harness.delegate.beans.DelegateProfileParams;
import io.harness.delegate.beans.DelegateRegisterResponse;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateScripts;
import io.harness.delegate.beans.DelegateTaskEvent;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.ConnectorHeartbeatDelegateResponse;
import io.harness.delegate.task.DelegateLogContext;
import io.harness.delegate.task.TaskLogContext;
import io.harness.exception.WingsException;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.managerclient.DelegateVersions;
import io.harness.managerclient.GetDelegatePropertiesRequest;
import io.harness.managerclient.GetDelegatePropertiesResponse;
import io.harness.managerclient.HttpsCertRequirement;
import io.harness.managerclient.WatcherVersion;
import io.harness.manifest.ManifestCollectionResponseHandler;
import io.harness.perpetualtask.PerpetualTaskLogContext;
import io.harness.perpetualtask.connector.ConnectorHearbeatPublisher;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.DelegateAuth;
import io.harness.serializer.KryoSerializer;

import software.wings.beans.Delegate;
import software.wings.delegatetasks.buildsource.BuildSourceExecutionResponse;
import software.wings.delegatetasks.manifest.ManifestCollectionExecutionResponse;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.url.SubdomainUrlHelperIntfc;
import software.wings.ratelimit.DelegateRequestRateLimiter;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.instance.InstanceHelper;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.DelegateService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.google.protobuf.Any;
import com.google.protobuf.TextFormat;
import com.google.protobuf.TextFormat.ParseException;
import io.swagger.annotations.Api;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

@Api("/agent/delegates")
@Path("/agent/delegates")
@Produces("application/json")
@Scope(DELEGATE)
@Slf4j
public class DelegateAgentResource {
  private DelegateService delegateService;
  private AccountService accountService;
  private WingsPersistence wingsPersistence;
  private DelegateRequestRateLimiter delegateRequestRateLimiter;
  private SubdomainUrlHelperIntfc subdomainUrlHelper;
  private ArtifactCollectionResponseHandler artifactCollectionResponseHandler;
  private InstanceHelper instanceHelper;
  private ManifestCollectionResponseHandler manifestCollectionResponseHandler;
  private ConnectorHearbeatPublisher connectorHearbeatPublisher;
  private KryoSerializer kryoSerializer;

  @Inject
  public DelegateAgentResource(DelegateService delegateService, AccountService accountService,
      WingsPersistence wingsPersistence, DelegateRequestRateLimiter delegateRequestRateLimiter,
      SubdomainUrlHelperIntfc subdomainUrlHelper, ArtifactCollectionResponseHandler artifactCollectionResponseHandler,
      InstanceHelper instanceHelper, ManifestCollectionResponseHandler manifestCollectionResponseHandler,
      ConnectorHearbeatPublisher connectorHearbeatPublisher, KryoSerializer kryoSerializer) {
    this.instanceHelper = instanceHelper;
    this.delegateService = delegateService;
    this.accountService = accountService;
    this.wingsPersistence = wingsPersistence;
    this.delegateRequestRateLimiter = delegateRequestRateLimiter;
    this.subdomainUrlHelper = subdomainUrlHelper;
    this.artifactCollectionResponseHandler = artifactCollectionResponseHandler;
    this.manifestCollectionResponseHandler = manifestCollectionResponseHandler;
    this.connectorHearbeatPublisher = connectorHearbeatPublisher;
    this.kryoSerializer = kryoSerializer;
  }

  @DelegateAuth
  @GET
  @Path("configuration")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateConfiguration> getDelegateConfiguration(
      @QueryParam("accountId") @NotEmpty String accountId) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      return new RestResponse<>(accountService.getDelegateConfiguration(accountId));
    }
  }

  @DelegateAuth
  @GET
  @Path("properties")
  @Timed
  @ExceptionMetered
  public RestResponse<String> getDelegateProperties(@QueryParam("request") @NotEmpty String request)
      throws ParseException {
    GetDelegatePropertiesRequest requestProto = TextFormat.parse(request, GetDelegatePropertiesRequest.class);
    String accountId = requestProto.getAccountId();
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      return new RestResponse<>(
          GetDelegatePropertiesResponse.newBuilder()
              .addAllResponseEntry(
                  requestProto.getRequestEntryList()
                      .stream()
                      .map(requestEntry -> {
                        switch (requestEntry.getTypeUrl().split("/")[1]) {
                          case "io.harness.managerclient.WatcherVersionQuery":
                            return Any.pack(
                                WatcherVersion.newBuilder()
                                    .setWatcherVersion(
                                        accountService.getDelegateConfiguration(accountId).getWatcherVersion())
                                    .build());
                          case "io.harness.managerclient.DelegateVersionsQuery":
                            return Any.pack(
                                DelegateVersions.newBuilder()
                                    .addAllDelegateVersion(
                                        accountService.getDelegateConfiguration(accountId).getDelegateVersions())
                                    .build());
                          case "io.harness.managerclient.HttpsCertRequirementQuery":
                            return Any.pack(
                                HttpsCertRequirement.newBuilder()
                                    .setCertRequirement(accountService.getHttpsCertificateRequirement(accountId))
                                    .build());
                          default:
                            throw new WingsException("invalid type: " + requestEntry.getTypeUrl());
                        }
                      })
                      .collect(toList()))
              .build()
              .toString());
    }
  }

  @DelegateAuth
  @POST
  @Path("register")
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
  @GET
  @Path("{delegateId}/profile")
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
  @Path("connectionHeartbeat/{delegateId}")
  @Timed
  @ExceptionMetered
  public void connectionHeartbeat(@QueryParam("accountId") @NotEmpty String accountId,
      @PathParam("delegateId") String delegateId, DelegateConnectionHeartbeat connectionHeartbeat) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      delegateService.registerHeartbeat(accountId, delegateId, connectionHeartbeat, ConnectionMode.POLLING);
    }
  }

  @POST
  public RestResponse<Delegate> add(@QueryParam("accountId") @NotEmpty String accountId, Delegate delegate) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      delegate.setAccountId(accountId);
      return new RestResponse<>(delegateService.add(delegate));
    }
  }

  @DelegateAuth
  @POST
  @Path("{delegateId}/tasks/{taskId}")
  @Consumes("application/x-kryo")
  @Timed
  @ExceptionMetered
  public void updateTaskResponse(@PathParam("delegateId") String delegateId, @PathParam("taskId") String taskId,
      @QueryParam("accountId") @NotEmpty String accountId, DelegateTaskResponse delegateTaskResponse) {
    try (AutoLogContext ignore1 = new TaskLogContext(taskId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore3 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      delegateService.processDelegateResponse(accountId, delegateId, taskId, delegateTaskResponse);
    }
  }

  @DelegateAuth
  @PUT
  @Produces("application/x-kryo")
  @Path("{delegateId}/tasks/{taskId}/acquire")
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
      return delegateService.acquireDelegateTask(accountId, delegateId, taskId);
    }
  }

  @DelegateAuth
  @POST
  @Produces("application/x-kryo")
  @Path("{delegateId}/tasks/{taskId}/report")
  @Timed
  @ExceptionMetered
  public DelegateTaskPackage reportConnectionResults(@PathParam("delegateId") String delegateId,
      @PathParam("taskId") String taskId, @QueryParam("accountId") @NotEmpty String accountId,
      List<DelegateConnectionResult> results) {
    try (AutoLogContext ignore1 = new TaskLogContext(taskId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore3 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      return delegateService.reportConnectionResults(accountId, delegateId, taskId, results);
    }
  }

  @DelegateAuth
  @GET
  @Produces("application/x-kryo")
  @Path("{delegateId}/tasks/{taskId}/fail")
  @Timed
  @ExceptionMetered
  public void failIfAllDelegatesFailed(@PathParam("delegateId") String delegateId, @PathParam("taskId") String taskId,
      @QueryParam("accountId") @NotEmpty String accountId) {
    try (AutoLogContext ignore1 = new TaskLogContext(taskId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore3 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      delegateService.failIfAllDelegatesFailed(accountId, delegateId, taskId);
    }
  }

  @DelegateAuth
  @PUT
  @Path("{delegateId}/clear-cache")
  @Timed
  @ExceptionMetered
  public void clearCache(
      @PathParam("delegateId") @NotEmpty String delegateId, @QueryParam("accountId") @NotEmpty String accountId) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      delegateService.clearCache(accountId, delegateId);
    }
  }

  @DelegateAuth
  @GET
  @Path("{delegateId}/upgrade")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateScripts> checkForUpgrade(@Context HttpServletRequest request,
      @HeaderParam("Version") String version, @PathParam("delegateId") @NotEmpty String delegateId,
      @QueryParam("accountId") @NotEmpty String accountId) throws IOException {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      return new RestResponse<>(delegateService.getDelegateScripts(
          accountId, version, subdomainUrlHelper.getManagerUrl(request, accountId), getVerificationUrl(request)));
    }
  }

  @DelegateAuth
  @GET
  @Path("delegateScripts")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateScripts> getDelegateScripts(@Context HttpServletRequest request,
      @QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("delegateVersion") @NotEmpty String delegateVersion) throws IOException {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      return new RestResponse<>(delegateService.getDelegateScripts(accountId, delegateVersion,
          subdomainUrlHelper.getManagerUrl(request, accountId), getVerificationUrl(request)));
    }
  }

  @DelegateAuth
  @GET
  @Path("{delegateId}/task-events")
  @Timed
  @ExceptionMetered
  public List<DelegateTaskEvent> getDelegateTaskEvents(@PathParam("delegateId") @NotEmpty String delegateId,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("syncOnly") boolean syncOnly) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      return delegateService.getDelegateTaskEvents(accountId, delegateId, syncOnly);
    }
  }

  @DelegateAuth
  @POST
  @Path("heartbeat-with-polling")
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
  @Path("{delegateId}/state-executions")
  @Timed
  @ExceptionMetered
  public void saveApiCallLogs(
      @PathParam("delegateId") String delegateId, @QueryParam("accountId") String accountId, byte[] logsBlob) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      log.info("About to convert logsBlob byte array into ThirdPartyApiCallLog.");
      List<ThirdPartyApiCallLog> logs = (List<ThirdPartyApiCallLog>) kryoSerializer.asObject(logsBlob);
      log.info("LogsBlob byte array converted successfully into ThirdPartyApiCallLog.");

      wingsPersistence.save(logs);
    }
  }

  private String getVerificationUrl(HttpServletRequest request) {
    return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
  }

  @DelegateAuth
  @POST
  @Path("artifact-collection/{perpetualTaskId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> processArtifactCollectionResult(
      @PathParam("perpetualTaskId") @NotEmpty String perpetualTaskId,
      @QueryParam("accountId") @NotEmpty String accountId, byte[] response) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new PerpetualTaskLogContext(perpetualTaskId, OVERRIDE_ERROR)) {
      BuildSourceExecutionResponse executionResponse = (BuildSourceExecutionResponse) kryoSerializer.asObject(response);

      if (executionResponse.getBuildSourceResponse() != null) {
        log.info("Received artifact collection {}", executionResponse.getBuildSourceResponse().getBuildDetails());
      }
      artifactCollectionResponseHandler.processArtifactCollectionResult(accountId, perpetualTaskId, executionResponse);
    }
    return new RestResponse<>(true);
  }

  @DelegateAuth
  @POST
  @Path("instance-sync/{perpetualTaskId}")
  public RestResponse<Boolean> processInstanceSyncResult(@PathParam("perpetualTaskId") @NotEmpty String perpetualTaskId,
      @QueryParam("accountId") @NotEmpty String accountId, DelegateResponseData response) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new PerpetualTaskLogContext(perpetualTaskId, OVERRIDE_ERROR)) {
      instanceHelper.processInstanceSyncResponseFromPerpetualTask(perpetualTaskId.replaceAll("[\r\n]", ""), response);
    } catch (Exception e) {
      log.error("Failed to process results for perpetual task: [{}]", perpetualTaskId.replaceAll("[\r\n]", ""), e);
    }
    return new RestResponse<>(true);
  }

  @DelegateAuth
  @POST
  @Path("manifest-collection/{perpetualTaskId}")
  public RestResponse<Boolean> processManifestCollectionResult(
      @PathParam("perpetualTaskId") @NotEmpty String perpetualTaskId,
      @QueryParam("accountId") @NotEmpty String accountId, byte[] serializedExecutionResponse) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new PerpetualTaskLogContext(perpetualTaskId, OVERRIDE_ERROR)) {
      ManifestCollectionExecutionResponse executionResponse =
          (ManifestCollectionExecutionResponse) kryoSerializer.asObject(serializedExecutionResponse);

      if (executionResponse.getManifestCollectionResponse() != null) {
        log.info("Received manifest collection {}", executionResponse.getManifestCollectionResponse().getHelmCharts());
      }
      manifestCollectionResponseHandler.handleManifestCollectionResponse(accountId, perpetualTaskId, executionResponse);
    }
    return new RestResponse<>(Boolean.TRUE);
  }

  @DelegateAuth
  @POST
  @Path("connectors/{perpetualTaskId}")
  public RestResponse<Boolean> publishNGConnectorHeartbeatResult(
      @PathParam("perpetualTaskId") @NotEmpty String perpetualTaskId,
      @QueryParam("accountId") @NotEmpty String accountId, ConnectorHeartbeatDelegateResponse validationResult) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new PerpetualTaskLogContext(perpetualTaskId, OVERRIDE_ERROR)) {
      connectorHearbeatPublisher.pushConnectivityCheckActivity(accountId, validationResult);
    }
    return new RestResponse<>(true);
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
        .sampleDelegate(delegateParams.isSampleDelegate())
        .currentlyExecutingDelegateTasks(delegateParams.getCurrentlyExecutingDelegateTasks())
        .location(delegateParams.getLocation())
        .build();
  }
}

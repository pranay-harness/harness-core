package software.wings.resources;

import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static software.wings.security.PermissionAttribute.ResourceType.DELEGATE;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.artifact.ArtifactCollectionResponseHandler;
import io.harness.beans.DelegateTaskEventsResponse;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateConfiguration;
import io.harness.delegate.beans.DelegateConfiguration.Action;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateScripts;
import io.harness.delegate.beans.DelegateTaskEvent;
import io.harness.delegate.beans.connector.ConnectorHeartbeatDelegateResponse;
import io.harness.delegate.task.DelegateLogContext;
import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.manifest.ManifestCollectionResponseHandler;
import io.harness.network.SafeHttpCall;
import io.harness.perpetualtask.PerpetualTaskLogContext;
import io.harness.perpetualtask.connector.ConnectorHearbeatPublisher;
import io.harness.perpetualtask.instancesync.InstanceSyncResponsePublisher;
import io.harness.persistence.HPersistence;
import io.harness.polling.client.PollingResourceClient;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.DelegateAuth;
import io.harness.serializer.KryoSerializer;

import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.delegatetasks.buildsource.BuildSourceExecutionResponse;
import software.wings.delegatetasks.manifest.ManifestCollectionExecutionResponse;
import software.wings.helpers.ext.url.SubdomainUrlHelperIntfc;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.instance.InstanceHelper;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.DelegateTaskServiceClassic;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.hibernate.validator.constraints.NotEmpty;

@Api("/agent/delegates")
@Path("/agent/delegates")
@Produces("application/json")
@Scope(DELEGATE)
@Slf4j
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
@OwnedBy(DEL)
@BreakDependencyOn("software.wings.service.impl.instance.InstanceHelper")
public class DelegateAgentResource {
  private DelegateService delegateService;
  private AccountService accountService;
  private HPersistence persistence;
  private SubdomainUrlHelperIntfc subdomainUrlHelper;
  private ArtifactCollectionResponseHandler artifactCollectionResponseHandler;
  private InstanceHelper instanceHelper;
  private ManifestCollectionResponseHandler manifestCollectionResponseHandler;
  private ConnectorHearbeatPublisher connectorHearbeatPublisher;
  private KryoSerializer kryoSerializer;
  private ConfigurationController configurationController;
  private FeatureFlagService featureFlagService;
  private DelegateTaskServiceClassic delegateTaskServiceClassic;
  private InstanceSyncResponsePublisher instanceSyncResponsePublisher;
  private PollingResourceClient pollingResourceClient;

  @Inject
  public DelegateAgentResource(DelegateService delegateService, AccountService accountService, HPersistence persistence,
      SubdomainUrlHelperIntfc subdomainUrlHelper, ArtifactCollectionResponseHandler artifactCollectionResponseHandler,
      InstanceHelper instanceHelper, ManifestCollectionResponseHandler manifestCollectionResponseHandler,
      ConnectorHearbeatPublisher connectorHearbeatPublisher, KryoSerializer kryoSerializer,
      ConfigurationController configurationController, FeatureFlagService featureFlagService,
      DelegateTaskServiceClassic delegateTaskServiceClassic, PollingResourceClient pollingResourceClient,
      InstanceSyncResponsePublisher instanceSyncResponsePublisher) {
    this.instanceHelper = instanceHelper;
    this.delegateService = delegateService;
    this.accountService = accountService;
    this.persistence = persistence;
    this.subdomainUrlHelper = subdomainUrlHelper;
    this.artifactCollectionResponseHandler = artifactCollectionResponseHandler;
    this.manifestCollectionResponseHandler = manifestCollectionResponseHandler;
    this.connectorHearbeatPublisher = connectorHearbeatPublisher;
    this.kryoSerializer = kryoSerializer;
    this.configurationController = configurationController;
    this.featureFlagService = featureFlagService;
    this.delegateTaskServiceClassic = delegateTaskServiceClassic;
    this.pollingResourceClient = pollingResourceClient;
    this.instanceSyncResponsePublisher = instanceSyncResponsePublisher;
  }

  @DelegateAuth
  @GET
  @Path("configuration")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateConfiguration> getDelegateConfiguration(
      @QueryParam("accountId") @NotEmpty String accountId) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      DelegateConfiguration configuration = accountService.getDelegateConfiguration(accountId);
      String primaryDelegateVersion = configurationController.getPrimaryVersion();
      // Adding primary delegate to the last element of delegate versions.
      if (isNotEmpty(configuration.getDelegateVersions())
          && configuration.getDelegateVersions().remove(primaryDelegateVersion)) {
        configuration.getDelegateVersions().add(primaryDelegateVersion);
      }
      return new RestResponse<>(configuration);
    } catch (InvalidRequestException ex) {
      if (isNotBlank(ex.getMessage()) && ex.getMessage().startsWith("Deleted AccountId")) {
        return new RestResponse<>(DelegateConfiguration.builder().action(Action.SELF_DESTRUCT).build());
      }

      return null;
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
  @GET
  @Path("{delegateId}/upgrade")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateScripts> checkForUpgrade(@Context HttpServletRequest request,
      @HeaderParam("Version") String version, @PathParam("delegateId") @NotEmpty String delegateId,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("delegateName") String delegateName)
      throws IOException {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      return new RestResponse<>(delegateService.getDelegateScripts(accountId, version,
          subdomainUrlHelper.getManagerUrl(request, accountId), getVerificationUrl(request), delegateName));
    }
  }

  @DelegateAuth
  @GET
  @Path("delegateScriptsNg")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateScripts> getDelegateScriptsNg(@Context HttpServletRequest request,
      @QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("delegateVersion") @NotEmpty String delegateVersion, @QueryParam("patchVersion") String patchVersion)
      throws IOException {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      String fullVersion = isNotEmpty(patchVersion) ? delegateVersion + "-" + patchVersion : delegateVersion;
      return new RestResponse<>(delegateService.getDelegateScriptsNg(
          accountId, fullVersion, subdomainUrlHelper.getManagerUrl(request, accountId), getVerificationUrl(request)));
    }
  }

  @DelegateAuth
  @GET
  @Path("delegateScripts")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateScripts> getDelegateScripts(@Context HttpServletRequest request,
      @QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("delegateVersion") @NotEmpty String delegateVersion, @QueryParam("patchVersion") String patchVersion,
      @QueryParam("delegateName") String delegateName) throws IOException {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      String fullVersion = isNotEmpty(patchVersion) ? delegateVersion + "-" + patchVersion : delegateVersion;
      return new RestResponse<>(delegateService.getDelegateScripts(accountId, fullVersion,
          subdomainUrlHelper.getManagerUrl(request, accountId), getVerificationUrl(request), delegateName));
    }
  }

  @DelegateAuth
  @GET
  @Path("{delegateId}/task-events")
  @Timed
  @ExceptionMetered
  public DelegateTaskEventsResponse getDelegateTaskEvents(@PathParam("delegateId") @NotEmpty String delegateId,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("syncOnly") boolean syncOnly) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      List<DelegateTaskEvent> delegateTaskEvents =
          delegateTaskServiceClassic.getDelegateTaskEvents(accountId, delegateId, syncOnly);
      return DelegateTaskEventsResponse.builder().delegateTaskEvents(delegateTaskEvents).build();
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
      log.debug("About to convert logsBlob byte array into ThirdPartyApiCallLog.");
      List<ThirdPartyApiCallLog> logs = (List<ThirdPartyApiCallLog>) kryoSerializer.asObject(logsBlob);
      log.debug("LogsBlob byte array converted successfully into ThirdPartyApiCallLog.");

      persistence.save(logs);
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
        log.debug("Received artifact collection {}", executionResponse.getBuildSourceResponse().getBuildDetails());
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
  @Path("instance-sync-ng/{perpetualTaskId}")
  public RestResponse<Boolean> processInstanceSyncNGResult(
      @PathParam("perpetualTaskId") @NotEmpty String perpetualTaskId,
      @QueryParam("accountId") @NotEmpty String accountId, DelegateResponseData response) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new PerpetualTaskLogContext(perpetualTaskId, OVERRIDE_ERROR)) {
      instanceSyncResponsePublisher.publishInstanceSyncResponseToNG(
          accountId, perpetualTaskId.replaceAll("[\r\n]", ""), response);
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
        log.debug("Received manifest collection {}", executionResponse.getManifestCollectionResponse().getHelmCharts());
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

  @DelegateAuth
  @POST
  @Path("polling/{perpetualTaskId}")
  public RestResponse<Boolean> processPollingResultNg(@PathParam("perpetualTaskId") @NotEmpty String perpetualTaskId,
      @QueryParam("accountId") @NotEmpty String accountId, byte[] serializedExecutionResponse) throws IOException {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new PerpetualTaskLogContext(perpetualTaskId, OVERRIDE_ERROR)) {
      SafeHttpCall.executeWithExceptions(pollingResourceClient.processPolledResult(perpetualTaskId, accountId,
          RequestBody.create(MediaType.parse("application/octet-stream"), serializedExecutionResponse)));
    }
    return new RestResponse<>(Boolean.TRUE);
  }
}

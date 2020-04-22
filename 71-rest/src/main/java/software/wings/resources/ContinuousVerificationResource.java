package software.wings.resources;

import static software.wings.common.VerificationConstants.COLLECT_CV_DATA;
import static software.wings.security.PermissionAttribute.Action.EXECUTE;
import static software.wings.security.PermissionAttribute.PermissionType.DEPLOYMENT;
import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.security.PermissionAttribute.ResourceType.SERVICE;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.ExecutionStatus;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import software.wings.common.VerificationConstants;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.CVCertifiedDetailsForWorkflowState;
import software.wings.service.impl.analysis.ContinuousVerificationService;
import software.wings.service.impl.analysis.DataCollectionInfoV2;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.apm.APMSetupTestNodeData;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateType;
import software.wings.verification.VerificationDataAnalysisResponse;

import java.util.List;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("apm")
@Path("/apm")
@Produces("application/json")
@Slf4j
public class ContinuousVerificationResource {
  @Inject private ContinuousVerificationService cvManagerService;

  @GET
  @Path("/verification-state-details")
  @Timed
  @Scope(SERVICE)
  @ExceptionMetered
  public RestResponse<StateExecutionData> getVerificationStateExecutionData(
      @QueryParam("accountId") final String accountId, @QueryParam("stateExecutionId") final String stateExecutionId) {
    return new RestResponse<>(cvManagerService.getVerificationStateExecutionData(stateExecutionId));
  }

  /**
   * Api to fetch Metric data for given node.
   * @param accountId
   * @param serverConfigId
   * @param fetchConfig
   * @return
   */
  @POST
  @Path("/node-data")
  @Timed
  @AuthRule(permissionType = LOGGED_IN)
  @ExceptionMetered
  public RestResponse<VerificationNodeDataSetupResponse> getMetricsWithDataForNode(
      @QueryParam("accountId") final String accountId, @QueryParam("serverConfigId") String serverConfigId,
      APMSetupTestNodeData fetchConfig) {
    return new RestResponse<>(
        cvManagerService.getDataForNode(accountId, serverConfigId, fetchConfig, StateType.APM_VERIFICATION));
  }

  @POST
  @Path(VerificationConstants.NOTIFY_VERIFICATION_STATE)
  @Timed
  @LearningEngineAuth
  public RestResponse<Boolean> notifyVerificationState(
      @QueryParam("correlationId") String correlationId, VerificationDataAnalysisResponse response) {
    return new RestResponse<>(cvManagerService.notifyVerificationState(correlationId, response));
  }

  @POST
  @Path(VerificationConstants.NOTIFY_WORKFLOW_VERIFICATION_STATE)
  @Timed
  @AuthRule(permissionType = DEPLOYMENT, action = EXECUTE)
  public RestResponse<Boolean> notifyWorkflowVerificationState(@QueryParam("accountId") String accountId,
      @Valid @QueryParam("appId") String appId, @Valid @QueryParam("workflowId") String workflowId,
      @Valid @QueryParam("workflowExecutionId") String workflowExecutionId,
      @Valid @QueryParam("stateExecutionId") String stateExecutionId,
      @Valid @QueryParam("status") ExecutionStatus status) {
    return new RestResponse<>(cvManagerService.notifyWorkflowVerificationState(appId, stateExecutionId, status));
  }

  @GET
  @Path(VerificationConstants.COLLECT_24_7_DATA)
  @Timed
  @LearningEngineAuth
  public RestResponse<Boolean> collect247CVData(@QueryParam("cvConfigId") String cvConfigId,
      @QueryParam("stateType") StateType stateType, @QueryParam("startTime") long startTime,
      @QueryParam("endTime") long endTime) {
    logger.info("Trigger Data Collection for 24x7 with cvConfigId {}, startTime {}, endTime {}", cvConfigId, startTime,
        endTime);
    return new RestResponse<>(cvManagerService.collect247Data(cvConfigId, stateType, startTime, endTime));
  }

  @GET
  @Path(VerificationConstants.COLLECT_DATA)
  @Timed
  @LearningEngineAuth
  public RestResponse<Boolean> collectWorkflowData(@QueryParam("analysisContextId") String contextId,
      @QueryParam("startDataCollectionMinute") long collectionMinute) {
    logger.info(
        "Trigger Data Collection for workflow with contextId {}, CollectionMinute {}", contextId, collectionMinute);
    return new RestResponse<>(cvManagerService.collectCVDataForWorkflow(contextId, collectionMinute));
  }

  @POST
  @Path(COLLECT_CV_DATA)
  @Timed
  @LearningEngineAuth
  public RestResponse<Boolean> collectCVData(
      @QueryParam("cvTaskId") String cvTaskId, DataCollectionInfoV2 dataCollectionInfoV2) {
    return new RestResponse<>(cvManagerService.collectCVData(cvTaskId, dataCollectionInfoV2));
  }

  @GET
  @Path(VerificationConstants.GET_CV_CERTIFIED_DETAILS_WORKFLOW)
  @Timed
  public RestResponse<List<CVCertifiedDetailsForWorkflowState>> getCVCertifiedLabelsForWorkflow(
      @QueryParam("accountId") String accountId, @QueryParam("appId") String appId,
      @QueryParam("workflowExecutionId") String workflowExecutionId) {
    return new RestResponse<>(cvManagerService.getCVCertifiedDetailsForWorkflow(accountId, appId, workflowExecutionId));
  }

  @GET
  @Path(VerificationConstants.GET_CV_CERTIFIED_DETAILS_PIPELINE)
  @Timed
  public RestResponse<List<CVCertifiedDetailsForWorkflowState>> getCVCertifiedLabelsForPipeline(
      @QueryParam("accountId") String accountId, @QueryParam("appId") String appId,
      @QueryParam("pipelineExecutionId") String pipelineExecutionId) {
    return new RestResponse<>(cvManagerService.getCVCertifiedDetailsForPipeline(accountId, appId, pipelineExecutionId));
  }
}

/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import io.harness.rest.RestResponse;

import software.wings.common.VerificationConstants;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.ExpAnalysisInfo;
import software.wings.service.impl.analysis.ExperimentalMessageComparisonResult;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.intfc.analysis.ExperimentalAnalysisService;
import software.wings.sm.StateType;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api(VerificationConstants.LEARNING_EXP_URL)
@Path("/" + VerificationConstants.LEARNING_EXP_URL)
@Produces("application/json")
@Scope(ResourceType.SETTING)
public class ExperimentalLogResource {
  @Inject private ExperimentalAnalysisService analysisService;

  @GET
  @Path(VerificationConstants.ANALYSIS_STATE_GET_EXP_ANALYSIS_INFO_URL)
  @Timed
  @ExceptionMetered
  public RestResponse<List<ExpAnalysisInfo>> getLogExpAnalysisInfo(@QueryParam("accountId") String accountId)
      throws IOException {
    return new RestResponse<>(analysisService.getLogExpAnalysisInfoList());
  }

  @GET
  @Path(VerificationConstants.ANALYSIS_STATE_GET_ANALYSIS_SUMMARY_URL)
  @Timed
  @ExceptionMetered
  public RestResponse<LogMLAnalysisSummary> getLogAnalysisSummary(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("stateType") StateType stateType, @QueryParam("expName") String expName) throws IOException {
    return new RestResponse<>(
        analysisService.getExperimentalAnalysisSummary(stateExecutionId, applicationId, stateType, expName));
  }

  @GET
  @Path(VerificationConstants.MSG_PAIRS_TO_VOTE)
  @Timed
  @ExceptionMetered
  public RestResponse<List<ExperimentalMessageComparisonResult>> getMessageComparisonList(
      @QueryParam("accountId") String accountId, @QueryParam("serviceId") String serviceId) {
    return new RestResponse<>(analysisService.getMessagePairsToVote(serviceId));
  }

  @PUT
  @Path(VerificationConstants.MSG_PAIRS_TO_VOTE)
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> saveMessageComparisonList(@QueryParam("accountId") String accountId,
      @QueryParam("serviceId") String serviceId, Map<String, String> userVotes) {
    return new RestResponse<>(analysisService.saveMessagePairsToVote(serviceId, userVotes));
  }
}

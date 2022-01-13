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
import software.wings.service.impl.analysis.CVFeedbackRecord;
import software.wings.service.impl.analysis.LabeledLogRecord;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogLabel;
import software.wings.service.intfc.analysis.LogLabelingService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.List;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.apache.commons.lang3.tuple.Pair;
import retrofit2.http.Body;

@Api(VerificationConstants.LOG_CLASSIFY_URL)
@Path("/" + VerificationConstants.LOG_CLASSIFY_URL)
@Produces("application/json")
@Scope(ResourceType.SETTING)
public class LogClassificationDashboardResource {
  @Inject LogLabelingService logLabelingService;

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<List<LogDataRecord>> getLogRecordsToClassify(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(logLabelingService.getLogRecordsToClassify(accountId));
  }

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> saveLabeledRecord(@QueryParam("accountId") final String accountId,
      @QueryParam("labels") List<LogLabel> labels, @Body Object params) {
    logLabelingService.saveClassifiedLogRecord(null, labels, accountId, params);
    return new RestResponse<>(true);
  }

  @GET
  @Path(VerificationConstants.GET_CLASSIFY_LABELS_URL)
  @Timed
  @ExceptionMetered
  public RestResponse<List<LogLabel>> getLabels(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(logLabelingService.getLabels());
  }

  @GET
  @Path(VerificationConstants.GET_IGNORE_RECORDS_TO_CLASSIFY)
  @Timed
  @ExceptionMetered
  public RestResponse<List<CVFeedbackRecord>> getIgnoreFeedbacksToClassify(@QueryParam("accountId") String accountId,
      @QueryParam("serviceId") String serviceId, @QueryParam("envId") String envId) {
    return new RestResponse<>(logLabelingService.getCVFeedbackToClassify(accountId, serviceId));
  }

  @GET
  @Path(VerificationConstants.GET_SAMPLE_LABELS_IGNORE_FEEDBACK)
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, List<CVFeedbackRecord>>> getSampleLabelsForIgnoreFeedback(
      @QueryParam("accountId") String accountId, @QueryParam("serviceId") String serviceId,
      @QueryParam("envId") String envId) {
    return new RestResponse<>(logLabelingService.getLabeledSamplesForIgnoreFeedback(accountId, serviceId, envId));
  }

  @GET
  @Path(VerificationConstants.GET_GLOBAL_IGNORE_RECORDS_TO_CLASSIFY)
  @Timed
  @ExceptionMetered
  public RestResponse<List<CVFeedbackRecord>> getGlobalFeedbacksToClassify(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(logLabelingService.getCVFeedbackToClassify(accountId));
  }

  @GET
  @Path(VerificationConstants.GET_GLOBAL_SAMPLE_LABELS_IGNORE_FEEDBACK)
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, List<CVFeedbackRecord>>> getGlobalSampleLabelsForCvFeedback(
      @QueryParam("accountId") String accountId) {
    return new RestResponse<>(logLabelingService.getLabeledSamplesForIgnoreFeedback(accountId));
  }

  @POST
  @Path(VerificationConstants.GET_IGNORE_RECORDS_TO_CLASSIFY)
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> saveLabeledIgnoreFeedback(
      @QueryParam("accountId") String accountId, @QueryParam("label") String label, CVFeedbackRecord feedbackRecord) {
    return new RestResponse<>(logLabelingService.saveLabeledIgnoreFeedback(accountId, feedbackRecord, label));
  }

  @POST
  @Path(VerificationConstants.POST_CLASSIFY_LABELS_LIST_URL)
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> saveLabeledIgnoreFeedbackList(
      @QueryParam("accountId") String accountId, Map<String, List<CVFeedbackRecord>> feedbackRecordMap) {
    return new RestResponse<>(logLabelingService.saveLabeledIgnoreFeedback(accountId, feedbackRecordMap));
  }

  @GET
  @Path(VerificationConstants.GET_ACCOUNTS_WITH_FEEDBACK)
  @Timed
  @ExceptionMetered
  public RestResponse<Map<Pair<String, String>, Integer>> getAccountsWithFeedback(
      @QueryParam("accountId") String accountId) {
    return new RestResponse<>(logLabelingService.getAccountsWithFeedback());
  }

  @GET
  @Path(VerificationConstants.GET_SERVICES_WITH_FEEDBACK)
  @Timed
  @ExceptionMetered
  public RestResponse<Map<Pair<String, String>, Integer>> getServicesWithFeedback(
      @QueryParam("accountId") String accountId) {
    return new RestResponse<>(logLabelingService.getServicesWithFeedbackForAccount(accountId));
  }

  @GET
  @Path(VerificationConstants.GET_SAMPLE_FEEDBACK_L2)
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, List<String>>> getSampleFeedbackL2(@QueryParam("accountId") String accountId,
      @QueryParam("serviceId") String serviceId, @QueryParam("envId") String envId) {
    return new RestResponse<>(logLabelingService.getSampleLabeledRecords(serviceId, envId));
  }

  @GET
  @Path(VerificationConstants.GET_L2_TO_CLASSIFY)
  @Timed
  @ExceptionMetered
  public RestResponse<List<LogDataRecord>> getL2RecordsToClassify(@QueryParam("accountId") String accountId,
      @QueryParam("serviceId") String serviceId, @QueryParam("envId") String envId) {
    return new RestResponse<>(logLabelingService.getL2RecordsToClassify(serviceId, envId));
  }

  @POST
  @Path(VerificationConstants.SAVE_LABELED_L2_FEEDBACK)
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> saveLabeledL2Feedback(@QueryParam("accountId") String accountId,
      @QueryParam("serviceId") String serviceId, @QueryParam("envId") String envId,
      List<LabeledLogRecord> labeledLogRecords) {
    return new RestResponse<>(logLabelingService.saveLabeledL2AndFeedback(labeledLogRecords));
  }
}

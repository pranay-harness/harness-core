package software.wings.resources;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.impl.newrelic.NewRelicApplicationInstance;
import software.wings.service.impl.newrelic.NewRelicMetric;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by rsingh on 09/05/17.
 *
 * We are versioning the Learning engine apis using the Accept Header. Clients can
 * request a specific version of the api by sending the version in the Accept header
 * as follows:
 *
 * Accept: application/v1+json, application/json
 *
 * This requests for version v1 of the api, and if that is not available it asks the server
 * to send the default api response. This is indicated by appending application/json to the
 * Accept header.
 *
 * Created 2 test apis test1 and test2 to show how to create versioned apis. Note that test1 serves
 * v1 and test2 serves v2 of the same api /test. Note that only the latest version v2 has 2 entries
 * in produces - one with a version and another with just application/json. The application/json
 * without the api version makes it the default implementation for /test api. So clients with no Accept
 * header and clients requesting just application/json will be served by the default api.
 *
 * IMPORTANT: Make sure that the latest version that is added is made the default api. There should
 * only be one default api.
 *
 *
 * For now, only apis used by the learning engine are versioned.
 */
@Api("newrelic")
@Path("/newrelic")
@Produces("application/json")
@Scope(ResourceType.SETTING)
public class NewRelicResource {
  @Inject private NewRelicService newRelicService;

  @Inject private MetricDataAnalysisService metricDataAnalysisService;

  @Inject private LearningEngineService learningEngineService;

  @Produces({"application/json", "application/v2+json"})
  @GET
  @Path("/test")
  @Timed
  @ExceptionMetered
  public RestResponse<String> test2(@QueryParam("accountId") String accountId) throws IOException {
    Map<String, Object> metaData = new HashMap<>();
    metaData.put("version", "v2");
    return RestResponse.Builder.aRestResponse().withResource("v2").withMetaData(metaData).build();
  }

  @Produces("application/v1+json")
  @GET
  @Path("/test")
  @Timed
  @ExceptionMetered
  public RestResponse<String> test1(@QueryParam("accountId") String accountId) throws IOException {
    return new RestResponse<>("v1");
  }

  @GET
  @Path("/applications")
  @Timed
  @ExceptionMetered
  public RestResponse<List<NewRelicApplication>> getAllApplications(
      @QueryParam("accountId") String accountId, @QueryParam("settingId") final String settingId) throws IOException {
    List<NewRelicApplication> applications = newRelicService.getApplications(settingId, StateType.NEW_RELIC);
    if (!isEmpty(applications)) {
      Collections.sort(applications);
    }
    return new RestResponse<>(applications);
  }

  @GET
  @Path("/nodes")
  @Timed
  @ExceptionMetered
  public RestResponse<List<NewRelicApplicationInstance>> getApplicationInstances(
      @QueryParam("accountId") String accountId, @QueryParam("settingId") final String settingId,
      @QueryParam("applicationId") final long applicationId) throws IOException {
    return new RestResponse<>(newRelicService.getApplicationInstances(settingId, applicationId, StateType.NEW_RELIC));
  }

  @POST
  @Path("/save-metrics")
  @Timed
  @DelegateAuth
  @ExceptionMetered
  public RestResponse<Boolean> saveMetricData(@QueryParam("accountId") final String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("delegateTaskId") String delegateTaskId, List<NewRelicMetricDataRecord> metricData)
      throws IOException {
    return new RestResponse<>(metricDataAnalysisService.saveMetricData(
        accountId, applicationId, stateExecutionId, delegateTaskId, metricData));
  }

  @GET
  @Path("/txns-with-data")
  @Timed
  @ExceptionMetered
  public RestResponse<List<NewRelicMetric>> getTxnsWithData(@QueryParam("accountId") String accountId,
      @QueryParam("settingId") final String settingId, @QueryParam("applicationId") final long applicationId,
      @QueryParam("instanceId") long instanceId) throws IOException {
    return new RestResponse<>(newRelicService.getTxnsWithData(settingId, applicationId, instanceId));
  }

  @GET
  @Path("/node-data")
  @Timed
  @DelegateAuth
  @ExceptionMetered
  public RestResponse<VerificationNodeDataSetupResponse> getMetricsWithDataForNode(
      @QueryParam("accountId") final String accountId, @QueryParam("settingId") final String settingId,
      @QueryParam("applicationId") final long applicationId, @QueryParam("instanceId") long instanceId,
      @QueryParam("from") long fromTime, @QueryParam("to") long toTime) {
    if (toTime <= 0 || fromTime <= 0) {
      toTime = System.currentTimeMillis();
      fromTime = toTime - TimeUnit.MINUTES.toMillis(15);
    }
    return new RestResponse<>(
        newRelicService.getMetricsWithDataForNode(settingId, applicationId, instanceId, fromTime, toTime));
  }
}

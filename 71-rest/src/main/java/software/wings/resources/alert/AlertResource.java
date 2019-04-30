package software.wings.resources.alert;

import static java.util.stream.Collectors.toList;
import static software.wings.beans.alert.AlertType.CONTINUOUS_VERIFICATION_ALERT;
import static software.wings.security.PermissionAttribute.ResourceType.ROLE;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.notifications.AlertVisibilityChecker;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import retrofit2.http.Body;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.Alert.AlertKeys;
import software.wings.beans.alert.cv.ContinuousVerificationAlertData;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.ContinuousVerificationService;
import software.wings.service.intfc.AlertService;

import java.util.List;
import java.util.Set;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by brett on 10/18/17
 */
@Api("alerts")
@Path("/alerts")
@Produces("application/json")
@Scope(ROLE)
public class AlertResource {
  @Inject private AlertService alertService;
  @Inject private AlertVisibilityChecker alertVisibilityChecker;
  @Inject private ContinuousVerificationService continuousVerificationService;

  // PL-1389
  private static final Set<software.wings.beans.alert.AlertType> ALERT_TYPES_TO_NOT_SHOW_UNDER_BELL_ICON =
      ImmutableSet.of(CONTINUOUS_VERIFICATION_ALERT);

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Alert>> list(
      @QueryParam("accountId") String accountId, @BeanParam PageRequest<Alert> request) {
    request.addFilter(AlertKeys.type, Operator.NOT_IN, ALERT_TYPES_TO_NOT_SHOW_UNDER_BELL_ICON.toArray());
    return new RestResponse<>(alertService.list(request));
  }

  @GET
  @Path("/types")
  public RestResponse<List<AlertType>> listCategoriesAndTypes(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(
        alertService.listCategoriesAndTypes(accountId).stream().map(AlertType::new).collect(toList()));
  }

  @POST
  @Path("/open-cv-alert")
  @LearningEngineAuth
  public RestResponse<Boolean> openCVAlert(
      @QueryParam("cvConfigId") String cvConfigId, @Body ContinuousVerificationAlertData alertData) {
    return new RestResponse<>(continuousVerificationService.openAlert(cvConfigId, alertData));
  }
}

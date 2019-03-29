package software.wings.resources.alert;

import static software.wings.security.PermissionAttribute.ResourceType.ROLE;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.notifications.AlertVisibilityChecker;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import retrofit2.http.Body;
import software.wings.beans.User;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.cv.ContinuousVerificationAlertData;
import software.wings.security.UserThreadLocal;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.AlertServiceImpl;
import software.wings.service.impl.analysis.ContinuousVerificationService;
import software.wings.service.intfc.AlertService;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
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

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Alert>> list(
      @QueryParam("accountId") String accountId, @BeanParam PageRequest<Alert> request) {
    User user = UserThreadLocal.get();
    if (null == user) {
      throw new WingsException(ErrorCode.USER_DOES_NOT_EXIST, "Could not find user while trying to list alerts");
    }

    PageResponse<Alert> response = alertService.list(request);
    List<Alert> filteredAlerts =
        response.stream()
            .filter(alert -> alertVisibilityChecker.shouldAlertBeShownToUser(accountId, alert, user))
            .collect(Collectors.toList());

    response.setResponse(filteredAlerts);
    return new RestResponse<>(response);
  }

  @GET
  @Path("/types")
  public RestResponse<List<AlertType>> listCategoriesAndTypes(@QueryParam("accountId") String accountId) {
    List<AlertType> types = Arrays.stream(software.wings.beans.alert.AlertType.values())
                                .filter(AlertServiceImpl.ALERT_TYPES_TO_NOTIFY_ON::contains)
                                .map(AlertType::new)
                                .collect(Collectors.toList());

    return new RestResponse<>(types);
  }

  @POST
  @Path("/open-cv-alert")
  @LearningEngineAuth
  public RestResponse<Boolean> openCVAlert(
      @QueryParam("cvConfigId") String cvConfigId, @Body ContinuousVerificationAlertData alertData) {
    return new RestResponse<>(continuousVerificationService.openAlert(cvConfigId, alertData));
  }
}

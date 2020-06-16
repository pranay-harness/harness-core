package software.wings.resources;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import com.google.inject.Inject;
import com.google.protobuf.util.Durations;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.logging.AutoLogContext;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.persistence.AccountLogContext;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import software.wings.service.intfc.AccountService;
import software.wings.utils.AccountPermissionUtils;

import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Slf4j
@Api("perpetual-task")
@Path("/perpetual-task")
@Produces(MediaType.APPLICATION_JSON)
public class SamplePTaskResource {
  @Inject private AccountService accountService;
  @Inject private PerpetualTaskService perpetualTaskService;
  @Inject private AccountPermissionUtils accountPermissionUtils;

  private static final String COUNTRY_NAME = "countryName";

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> create(
      @QueryParam("accountId") String accountId, @QueryParam("country") String countryName) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Creating a Sample Perpetual Task.");
      RestResponse<Boolean> response = accountPermissionUtils.checkIfHarnessUser("User not allowed to enable account");
      if (response == null) {
        Map<String, String> clientParamMap = new HashMap<>();
        clientParamMap.put(COUNTRY_NAME, countryName);
        PerpetualTaskClientContext clientContext = new PerpetualTaskClientContext(clientParamMap);
        PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                             .setInterval(Durations.fromMinutes(1))
                                             .setTimeout(Durations.fromSeconds(30))
                                             .build();
        String taskId =
            perpetualTaskService.createTask(PerpetualTaskType.SAMPLE, accountId, clientContext, schedule, false);
        response = new RestResponse<>(taskId != null);
      }
      return response;
    }
  }
}
package io.harness.ng.core.remote;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.NgDelegateServiceGrpcClient;
import io.harness.delegate.SendTaskResponse;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.hibernate.validator.constraints.NotBlank;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Path("/delegate-tasks")
@Api("/delegate-tasks")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
public class NGDelegateClientResource {
  private static final String HTTP_URL_200 = "http://httpstat.us/200";
  private final NgDelegateServiceGrpcClient ngDelegateServiceGrpcClient;

  @Inject
  public NGDelegateClientResource(NgDelegateServiceGrpcClient ngDelegateServiceGrpcClient) {
    this.ngDelegateServiceGrpcClient = ngDelegateServiceGrpcClient;
  }

  @GET
  @ApiOperation(value = "Create a delegate tasks", nickname = "postDelegate")
  public String create(@QueryParam("accountId") @NotBlank String accountId) {
    Map<String, String> setupAbstractions = ImmutableMap.of("accountId", accountId);
    TaskData taskData =
        TaskData.builder()
            .async(true)
            .taskType("HTTP")
            .timeout(TimeUnit.MINUTES.toMillis(1))
            .parameters(new Object[] {HttpTaskParameters.builder().method("GET").url(HTTP_URL_200).build()})
            .build();
    SendTaskResponse sendTaskResponse =
        this.ngDelegateServiceGrpcClient.sendTask(accountId, setupAbstractions, taskData);
    return sendTaskResponse.getTaskId().getId();
  }
}

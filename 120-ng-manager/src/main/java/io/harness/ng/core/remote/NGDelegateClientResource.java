package io.harness.ng.core.remote;

import com.google.inject.Inject;

import io.harness.NgDelegateTaskGrpcClient;
import io.harness.delegate.SendTaskRequest;
import io.harness.delegate.SendTaskResponse;
import io.harness.delegate.TaskId;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/delegate-tasks")
@Api("/delegate-tasks")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
public class NGDelegateClientResource {
  private final NgDelegateTaskGrpcClient ngDelegateServiceGrpcClient;

  @Inject
  public NGDelegateClientResource(NgDelegateTaskGrpcClient ngDelegateServiceGrpcClient) {
    this.ngDelegateServiceGrpcClient = ngDelegateServiceGrpcClient;
  }

  @GET
  @ApiOperation(value = "Create a delegate tasks", nickname = "postDelegate")
  public String create() {
    SendTaskResponse sendTaskResponse = this.ngDelegateServiceGrpcClient.sendTask(
        SendTaskRequest.newBuilder().setTaskId(TaskId.newBuilder().setId("taskId")).build());
    return sendTaskResponse.getTaskId().getId();
  }
}

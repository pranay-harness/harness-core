package io.harness.app.resources;

import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.grpc.StatusRuntimeException;
import io.harness.app.intfc.CIPipelineService;
import io.harness.exception.InvalidRequestException;
import io.harness.impl.CIPipelineExecutionService;
import io.harness.product.ci.scm.proto.GitProvider;
import io.harness.product.ci.scm.proto.Header;
import io.harness.product.ci.scm.proto.ParseWebhookRequest;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;
import io.harness.product.ci.scm.proto.PullRequestHook;
import io.harness.product.ci.scm.proto.PushHook;
import io.harness.product.ci.scm.proto.SCMGrpc;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.PublicApi;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.WebHookRequest;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

@Api("ci")
@Path("/ci")
@Produces("application/json")
@PublicApi
@Slf4j
public class CIWebhookTriggerResource {
  private static final String X_GIT_HUB_EVENT = "X-GitHub-Event";
  private static final String X_GIT_LAB_EVENT = "X-Gitlab-Event";
  private static final String X_BIT_BUCKET_EVENT = "X-Event-Key";

  private CIPipelineService ciPipelineService;
  private CIPipelineExecutionService ciPipelineExecutionService;
  @Inject private SCMGrpc.SCMBlockingStub scmBlockingStub;

  @GET
  @Timed
  @ExceptionMetered
  @Path("{id}")
  public Response ping(@PathParam("id") String webHookToken, WebHookRequest webHookRequest) {
    return Response.status(Response.Status.OK).build();
  }

  @POST
  @Timed
  @ExceptionMetered
  @Path("/pipelines/{id}/trigger")
  public RestResponse<String> runPipelineFromTrigger(
      @PathParam("id") String pipelineId, String eventPayload, @Context HttpHeaders httpHeaders) {
    Header.Builder header = Header.newBuilder();
    httpHeaders.getRequestHeaders().forEach(
        (k, v) -> { header.addFields(Header.Pair.newBuilder().setKey(k).addAllValues(v).build()); });

    GitProvider gitProvider = obtainWebhookSource(httpHeaders);
    ParseWebhookResponse parseWebhookResponse;
    try {
      parseWebhookResponse = scmBlockingStub.parseWebhook(ParseWebhookRequest.newBuilder()
                                                              .setBody(eventPayload)
                                                              .setProvider(gitProvider)
                                                              .setHeader(header.build())
                                                              .build());
    } catch (StatusRuntimeException e) {
      logger.error("Failed to parse webhook payload", eventPayload);
      throw e;
    }

    logger.info(parseWebhookResponse.toString());
    if (parseWebhookResponse.hasPr()) {
      PullRequestHook prHook = parseWebhookResponse.getPr();
      logger.info(prHook.toString());
    } else if (parseWebhookResponse.hasPush()) {
      PushHook pushHook = parseWebhookResponse.getPush();
      logger.info(pushHook.toString());
    } else {
      logger.error("Unknown webhook event");
      throw new InvalidRequestException("Unknown webhook event", USER);
    }

    try {
      ciPipelineExecutionService.executePipeline(ciPipelineService.readPipeline(pipelineId));
    } catch (Exception e) {
      logger.error("Failed to run input pipeline ", e);
    }

    return null;
  }

  private GitProvider obtainWebhookSource(HttpHeaders httpHeaders) {
    if (httpHeaders == null) {
      throw new InvalidRequestException("Failed to resolve Webhook Source. Reason: HttpHeaders are empty.");
    }

    if (httpHeaders.getHeaderString(X_GIT_HUB_EVENT) != null) {
      return GitProvider.GITHUB;
    } else if (httpHeaders.getHeaderString(X_GIT_LAB_EVENT) != null) {
      return GitProvider.GITLAB;
    } else if (httpHeaders.getHeaderString(X_BIT_BUCKET_EVENT) != null) {
      return GitProvider.BITBUCKET;
    }
    throw new InvalidRequestException("Unable to resolve the Webhook Source. "
            + "One of " + X_GIT_HUB_EVENT + ", " + X_BIT_BUCKET_EVENT + ", " + X_GIT_LAB_EVENT
            + " must be present in Headers",
        USER);
  }
}

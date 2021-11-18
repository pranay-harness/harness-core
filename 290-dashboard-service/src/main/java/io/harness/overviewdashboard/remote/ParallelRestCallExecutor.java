package io.harness.overviewdashboard.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.context.GlobalContext;
import io.harness.manage.GlobalContextManager;
import io.harness.overviewdashboard.bean.RestCallRequest;
import io.harness.overviewdashboard.bean.RestCallResponse;
import io.harness.remote.client.NGRestUtils;

import groovy.lang.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(PL)
public class ParallelRestCallExecutor {
  private static int TIMEOUT_IN_SECONDS = 10;

  public List<RestCallResponse> executeRestCalls(List<RestCallRequest> restCallRequests) {
    Instant start = Instant.now();
    log.info("Starting the rest calls for the following requests {}",
        restCallRequests.stream()
            .map(restCallRequest -> restCallRequest.getRequestType().toString())
            .collect(Collectors.joining(", ")));

    List<CompletableFuture<RestCallResponse>> allFutures = new ArrayList<>();
    for (RestCallRequest restCallRequest : restCallRequests) {
      allFutures.add(getFutureForAPICallRequest(restCallRequest));
    }
    CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0]));

    List<RestCallResponse> restCallResponses = new ArrayList<>();
    for (CompletableFuture<RestCallResponse> future : allFutures) {
      try {
        restCallResponses.add(future.get(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS));
      } catch (Exception e) {
        log.error("Could not get the response from the microservice", e);
      }
    }

    log.info("Total time taken in the rest calls: " + Duration.between(start, Instant.now()).getSeconds());
    return restCallResponses;
  }

  <T> CompletableFuture<RestCallResponse<T>> getFutureForAPICallRequest(RestCallRequest<T> restCallRequest) {
    GlobalContext globalContext = GlobalContextManager.obtainGlobalContext();
    return CompletableFuture.supplyAsync(() -> executeRestCall(restCallRequest, globalContext));
  }

  <T> RestCallResponse<T> executeRestCall(RestCallRequest<T> restCallRequest, GlobalContext globalContext) {
    final T response;
    try {
      GlobalContextManager.set(globalContext);
      response = NGRestUtils.getResponse(restCallRequest.getRequest());
    } catch (Exception ex) {
      GlobalContextManager.unset();
      log.error("Error occured while performing the rest request {}", restCallRequest.getRequestType(), ex);
      return RestCallResponse.<T>builder().ex(ex).callFailed(true).build();
    } finally {
      GlobalContextManager.unset();
    }
    return RestCallResponse.<T>builder().requestType(restCallRequest.getRequestType()).response(response).build();
  }
}

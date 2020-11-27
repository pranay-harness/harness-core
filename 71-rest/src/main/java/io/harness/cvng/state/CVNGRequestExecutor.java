package io.harness.cvng.state;

import static software.wings.beans.AccountType.log;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;
import groovy.util.logging.Slf4j;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.web.client.HttpServerErrorException;
import retrofit2.Call;
import retrofit2.Response;
@Singleton
@Slf4j
public class CVNGRequestExecutor {
  private static final double DELAY_FACTOR = 1.57;
  @VisibleForTesting static Duration JITTER = Duration.ofMillis(100);
  public <U> U execute(Call<U> request) {
    int retryCount[] = {0};
    RetryPolicy<Object> retryPolicy =
        new RetryPolicy<>()
            .handle(Exception.class)
            .withBackoff(200, 10000, ChronoUnit.MILLIS, DELAY_FACTOR)
            .withMaxAttempts(3)
            .withJitter(JITTER)
            .abortOn(HttpServerErrorException.InternalServerError.class)
            .onFailedAttempt(event -> {
              retryCount[0]++;
              log.warn("[Retrying] Error while calling manager for call {}, retryCount: {}",
                  request.request().toString(), retryCount[0], event.getLastFailure());
            })
            .onFailure(event
                -> log.error("Error while calling manager for call {} after {} retries", request.request().toString(),
                    3, event.getFailure()));

    return Failsafe.with(retryPolicy).get(() -> executeRequest(request));
  }

  private <U> U executeRequest(Call<U> request) {
    try {
      Response<U> response = request.clone().execute();
      if (response.isSuccessful()) {
        return response.body();
      } else {
        String errorBody = response.errorBody().string();
        throw new IllegalStateException(
            "Code: " + response.code() + ", message: " + response.message() + ", body: " + errorBody);
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}

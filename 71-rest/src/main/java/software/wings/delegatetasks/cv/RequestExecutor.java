package software.wings.delegatetasks.cv;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.sleep;
import static software.wings.common.VerificationConstants.RATE_LIMIT_STATUS;
import static software.wings.common.VerificationConstants.URL_STRING;
import static software.wings.delegatetasks.cv.CVConstants.RETRY_SLEEP_DURATION;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okio.Buffer;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallField;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.ThreadLocalRandom;
@Slf4j
@Singleton
public class RequestExecutor {
  @Inject private DelegateLogService delegateLogService;
  private static final int MAX_RETRIES = 3;
  public <U> U executeRequest(ThirdPartyApiCallLog thirdPartyApiCallLog, Call<U> request) {
    int retryCount = 0;
    while (true) {
      try {
        return executeRequest(thirdPartyApiCallLog, retryCount, request);
      } catch (RateLimitExceededException e) {
        int randomNum = ThreadLocalRandom.current().nextInt(1, 5);
        logger.info("Encountered Rate limiting. Sleeping {} seconds", RETRY_SLEEP_DURATION.getSeconds() + randomNum);
        sleep(RETRY_SLEEP_DURATION.plus(Duration.ofSeconds(randomNum)));
        if (retryCount == MAX_RETRIES) {
          logger.error("Request did not succeed after " + MAX_RETRIES + "  retries ");
          throw new DataCollectionException(e);
        }
      } catch (Exception e) {
        if (retryCount == MAX_RETRIES) {
          logger.error("Request did not succeed after " + MAX_RETRIES + "  retries", e);
          throw new DataCollectionException(e);
        }
      }
      retryCount++;
    }
  }

  private String bodyToString(final Request request) {
    try {
      final Request copy = request.newBuilder().build();
      final Buffer buffer = new Buffer();
      copy.body().writeTo(buffer);
      return buffer.readUtf8();
    } catch (final IOException e) {
      throw new DataCollectionException(e);
    }
  }
  private <U> U executeRequest(ThirdPartyApiCallLog apiCallLog, int retryCount, Call<U> request) {
    apiCallLog = apiCallLog.copy();
    apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
    try {
      apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                       .name(URL_STRING)
                                       .value(request.request().url().toString())
                                       .type(FieldType.URL)
                                       .build());
      if (retryCount != 0) {
        apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                         .name("RETRY")
                                         .value(String.valueOf(retryCount))
                                         .type(FieldType.NUMBER)
                                         .build());
      }
      apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                       .name("METHOD")
                                       .value(request.request().method())
                                       .type(FieldType.TEXT)
                                       .build());
      if (request.request().body() != null) {
        String body = bodyToString(request.request());
        if (isNotEmpty(body)) {
          apiCallLog.addFieldToRequest(
              ThirdPartyApiCallField.builder().name("body").value(body).type(FieldType.JSON).build());
        }
      }
      Response<U> response = request.clone().execute(); // TODO: add retry logic and rate limit exceeded logic here.
      apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
      if (response.isSuccessful()) {
        apiCallLog.addFieldToResponse(response.code(), response.body(), FieldType.JSON);

      } else if (response.code() == RATE_LIMIT_STATUS) {
        apiCallLog.addFieldToResponse(response.code(), response.toString(), FieldType.TEXT);
        throw new RateLimitExceededException(
            "Response code: " + response.code() + " Error: " + response.errorBody().string());
      } else {
        apiCallLog.addFieldToResponse(response.code(), response.toString(), FieldType.TEXT);
        throw new DataCollectionException(
            "Response code: " + response.code() + " Error: " + response.errorBody().string());
      }
      return response.body();
    } catch (IOException e) {
      apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
      apiCallLog.addFieldToResponse(HttpStatus.SC_BAD_REQUEST, ExceptionUtils.getStackTrace(e), FieldType.TEXT);
      throw new DataCollectionException(e);
    } finally {
      delegateLogService.save(apiCallLog.getAccountId(), apiCallLog);
    }
  }
}

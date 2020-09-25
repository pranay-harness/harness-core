package io.harness.ng.core;

import lombok.experimental.UtilityClass;
import okhttp3.Interceptor;
import org.slf4j.MDC;

import javax.validation.constraints.NotNull;

@UtilityClass
public class CorrelationContext {
  private static final String correlationIdKey = "X-requestId";

  public static String getCorrelationIdKey() {
    return correlationIdKey;
  }

  public static String getCorrelationId() {
    return MDC.get(correlationIdKey);
  }

  public static void setCorrelationId(String correlationId) {
    MDC.put(correlationIdKey, correlationId);
  }

  public static void clearCorrelationId() {
    MDC.remove(correlationIdKey);
  }

  @NotNull
  public static Interceptor getCorrelationIdInterceptor() {
    return chain -> {
      return chain.proceed(chain.request().newBuilder().header(getCorrelationIdKey(), getCorrelationId()).build());
    };
  }
}

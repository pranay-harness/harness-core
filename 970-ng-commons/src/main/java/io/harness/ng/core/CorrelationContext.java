/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.validation.constraints.NotNull;
import lombok.experimental.UtilityClass;
import okhttp3.Interceptor;
import org.slf4j.MDC;

@OwnedBy(PL)
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
      String correlationId = getCorrelationId();
      if (correlationId == null) {
        correlationId = generateRandomUuid().toString();
      }
      return chain.proceed(chain.request().newBuilder().header(getCorrelationIdKey(), correlationId).build());
    };
  }

  public static UUID generateRandomUuid() {
    final Random rnd = ThreadLocalRandom.current();
    long mostSig = rnd.nextLong();
    long leastSig = rnd.nextLong();

    // Identify this as a version 4 UUID, that is one based on a random value.
    mostSig &= 0xffffffffffff0fffL;
    mostSig |= 0x0000000000004000L;

    // Set the variant identifier as specified for version 4 UUID values.  The two
    // high order bits of the lower word are required to be one and zero, respectively.
    leastSig &= 0x3fffffffffffffffL;
    leastSig |= 0x8000000000000000L;

    return new UUID(mostSig, leastSig);
  }
}

/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.telemetry.annotation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.telemetry.Destination;
import io.harness.telemetry.TelemetryReporter;
import io.harness.telemetry.annotation.utils.TelemetryEventUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

@OwnedBy(HarnessTeam.GTM)
@Slf4j
@Singleton
public class TrackEventInterceptor implements MethodInterceptor {
  @Inject private TelemetryReporter telemetryReporter;
  private static final String FAILED_INPUT = "";

  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {
    SendTrackEvent[] sendTrackEvents = methodInvocation.getMethod().getAnnotationsByType(SendTrackEvent.class);
    Arrays.stream(sendTrackEvents)
        .filter(s -> TriggerAt.PRE_METHOD.equals(s.triggerAt()))
        .forEach(s -> processTrackEvent(s, methodInvocation.getArguments()));

    Object result = methodInvocation.proceed();
    Arrays.stream(sendTrackEvents)
        .filter(s -> TriggerAt.POST_METHOD.equals(s.triggerAt()))
        .forEach(s -> processTrackEvent(s, methodInvocation.getArguments()));
    return result;
  }

  void processTrackEvent(SendTrackEvent sendTrackEvent, Object[] arguments) {
    String accountId = checkAndGetValue(sendTrackEvent.accountId(), arguments);
    if (FAILED_INPUT.equals(accountId)) {
      log.error("Failed to read input for accountId, won't send out the track event");
      return;
    }
    String identity = checkAndGetValue(sendTrackEvent.identity(), arguments);
    if (FAILED_INPUT.equals(identity)) {
      log.error("Failed to read input for identity, won't send out the track event");
      return;
    }

    HashMap<String, Object> properties = TelemetryEventUtils.generateProperties(sendTrackEvent.properties(), arguments);
    Map<Destination, Boolean> destinations = TelemetryEventUtils.generateDestinations(sendTrackEvent.destinations());
    telemetryReporter.sendTrackEvent(
        sendTrackEvent.eventName(), identity, accountId, properties, destinations, sendTrackEvent.category());
  }

  private String checkAndGetValue(Input input, Object[] arguments) {
    if (TelemetryEventUtils.isDefaultInput(input)) {
      // user doesn't change on the default input, use auto detect
      return null;
    }

    String value = TelemetryEventUtils.getValueFromInput(input, arguments);
    if (value == null) {
      // user indicated wrong value, do not send the event
      return FAILED_INPUT;
    }
    return value;
  }
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.telemetry.annotation;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.telemetry.Destination;
import io.harness.telemetry.TelemetryReporter;
import io.harness.telemetry.annotation.utils.TelemetryEventUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

@OwnedBy(HarnessTeam.GTM)
@Slf4j
@Singleton
public class IdentifyEventInterceptor implements MethodInterceptor {
  @Inject private TelemetryReporter telemetryReporter;

  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {
    SendIdentifyEvent[] sendIdentifyEvents = methodInvocation.getMethod().getAnnotationsByType(SendIdentifyEvent.class);
    SendIdentifyEvent identifyEvent = sendIdentifyEvents[0];
    if (TriggerAt.PRE_METHOD.equals(identifyEvent.triggerAt())) {
      processIdentifyEvent(identifyEvent, methodInvocation.getArguments());
    }

    Object result = methodInvocation.proceed();
    if (TriggerAt.POST_METHOD.equals(identifyEvent.triggerAt())) {
      processIdentifyEvent(identifyEvent, methodInvocation.getArguments());
    }
    return result;
  }

  void processIdentifyEvent(SendIdentifyEvent sendIdentifyEvent, Object[] arguments) {
    String identity = TelemetryEventUtils.getValueFromInput(sendIdentifyEvent.identity(), arguments);
    if (isEmpty(identity)) {
      log.error("Failed to send identify message, due to invalid information from identity input");
      return;
    }
    HashMap<String, Object> properties =
        TelemetryEventUtils.generateProperties(sendIdentifyEvent.properties(), arguments);
    Map<Destination, Boolean> destinations = TelemetryEventUtils.generateDestinations(sendIdentifyEvent.destinations());
    telemetryReporter.sendIdentifyEvent(identity, properties, destinations);
  }
}

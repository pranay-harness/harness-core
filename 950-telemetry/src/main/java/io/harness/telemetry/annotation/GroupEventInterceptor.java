/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.telemetry.annotation;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

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
public class GroupEventInterceptor implements MethodInterceptor {
  @Inject private TelemetryReporter telemetryReporter;

  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {
    SendGroupEvent[] sendGroupEvents = methodInvocation.getMethod().getAnnotationsByType(SendGroupEvent.class);
    SendGroupEvent groupEvent = sendGroupEvents[0];
    if (TriggerAt.PRE_METHOD.equals(groupEvent.triggerAt())) {
      processGroupEvent(groupEvent, methodInvocation.getArguments());
    }

    Object result = methodInvocation.proceed();
    if (TriggerAt.POST_METHOD.equals(groupEvent.triggerAt())) {
      processGroupEvent(groupEvent, methodInvocation.getArguments());
    }
    return result;
  }

  void processGroupEvent(SendGroupEvent groupEvent, Object[] arguments) {
    String accountId = TelemetryEventUtils.getValueFromInput(groupEvent.accountId(), arguments);
    if (accountId == null) {
      log.error("Failed to send group event, due to invalid information from accountId input");
      return;
    }
    HashMap<String, Object> properties = TelemetryEventUtils.generateProperties(groupEvent.properties(), arguments);
    Map<Destination, Boolean> destinations = TelemetryEventUtils.generateDestinations(groupEvent.destinations());

    if (TelemetryEventUtils.isDefaultInput(groupEvent.identity())) {
      // Uses default identity input
      telemetryReporter.sendGroupEvent(accountId, properties, destinations);
      return;
    }

    String identity = TelemetryEventUtils.getValueFromInput(groupEvent.identity(), arguments);
    if (isNotEmpty(identity)) {
      telemetryReporter.sendGroupEvent(accountId, identity, properties, destinations);
    } else {
      log.error("Failed to retrieve information from identity input. Will not send out the group event message");
    }
  }
}

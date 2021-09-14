/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.telemetry.annotation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.telemetry.Destination;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sending out a identity event.
 * If identify is defined with invalid value, message will not be sent.
 * for example,
 * &#064;SendIdentifyEvent(identity = &#064;Input(argumentIndex = 0),
 *       properties =
 *       {
 *         &#064;EventProperty(key = "groupId", value = &#064;Input(argumentIndex = 1))
 *       })
 * void example(String email, String accountId)
 *
 * Same as
 * TelemetryReporter.sendIdentifyEvent(email, ImmutableMap.builder()
 *             .put("groupId", accountId).build(), null);
 */
@OwnedBy(HarnessTeam.GTM)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SendIdentifyEvent {
  Input identity();
  Input accountId() default @Input;
  TriggerAt triggerAt() default TriggerAt.POST_METHOD;
  EventProperty[] properties() default {};
  Destination[] destinations() default {};
}

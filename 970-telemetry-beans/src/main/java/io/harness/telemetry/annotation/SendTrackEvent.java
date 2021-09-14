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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method annotated with this will automatically send out a telemetry track message,
 * identity can be explicitly set or infer from principal(User as email, api as apikey, service as serviceId).
 * If the identiy or accountId is defined with invalid value, message will not be sent
 *
 * for instance,
 * &#064;SendTrackEvent(eventName = "delete_start", category = Category.SIGNUP, triggerAt = TriggerAt.PRE_METHOD,
 * identity =
 * &#064;Input(argumentIndex = 0), properties =
 *       {
 *         &#064;EventProperty(key = "method", value = &#064;Input(value = "delete"))
 *       },
 *       destinations = Destination.NATERO)
 * void example(String identity);
 *
 * Same as
 * HashMap&lt;String, Object&gt; properties = new HashMap<>();
 * properties.put("method,"delete");
 * TelemetryReporter.sendTrackEvent("delete_start", identity, null,properties, ImmutableMap.builder()
 * .put(SegmentDestination.NATERO, true).put(SegmentDestination.ALL, false), Category.SIGNUP);
 */
@OwnedBy(HarnessTeam.GTM)
@Repeatable(SendTrackEvents.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SendTrackEvent {
  String eventName();
  String category();
  Input identity() default @Input;
  Input accountId() default @Input;
  TriggerAt triggerAt() default TriggerAt.POST_METHOD;
  EventProperty[] properties() default {};
  Destination[] destinations() default {};
}

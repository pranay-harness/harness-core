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

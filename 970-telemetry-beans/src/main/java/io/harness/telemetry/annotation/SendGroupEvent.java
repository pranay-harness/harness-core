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
 * Sending out a group event
 *
 * for example,
 * &#064;SendGroupEvent(accountId = &#064;Input("groupn"), properties = &#064;EventProperty(key = "test", value =
 * &#064;Input(value = "test")))
 *
 * same as
 * TelemetryReporter.sendGroupEvent("groupn", ImmutableMap.builder().put("test", "test").build(), null);
 *
 */
@OwnedBy(HarnessTeam.GTM)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SendGroupEvent {
  Input accountId();
  Input identity() default @Input;
  TriggerAt triggerAt() default TriggerAt.POST_METHOD;
  EventProperty[] properties() default {};
  Destination[] destinations() default {};
}

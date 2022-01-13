/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.telemetry.annotation.utils;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.telemetry.Destination;
import io.harness.telemetry.annotation.EventProperty;
import io.harness.telemetry.annotation.Input;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.GTM)
@UtilityClass
@Slf4j
public class TelemetryEventUtils {
  public static boolean isValidArgument(int index, Object[] arguments) {
    return arguments != null && index >= 0 && index < arguments.length && arguments[index] != null;
  }

  public static boolean isDefaultInput(Input input) {
    return input.argumentIndex() == -1 && isEmpty(input.value());
  }

  public static String getValueFromInput(Input input, Object[] arguments) {
    if (isNotEmpty(input.value())) {
      return input.value();
    }

    if (isValidArgument(input.argumentIndex(), arguments)) {
      return String.valueOf(arguments[input.argumentIndex()]);
    }
    return null;
  }

  public static HashMap<String, Object> generateProperties(EventProperty[] properties, Object[] arguments) {
    HashMap<String, Object> result = new HashMap<>();
    for (EventProperty eventProperty : properties) {
      String constantValue = eventProperty.value().value();
      int argIndex = eventProperty.value().argumentIndex();

      // add constant value or argument value to the key
      if (isNotEmpty(constantValue)) {
        result.put(eventProperty.key(), constantValue);
      } else {
        if (isValidArgument(argIndex, arguments)) {
          result.put(eventProperty.key(), arguments[argIndex]);
        } else {
          log.debug("argIndex {} is invalid, property with key {} is omitted", argIndex, eventProperty.key());
        }
      }
    }
    return result;
  }

  public static Map<Destination, Boolean> generateDestinations(Destination[] destinations) {
    Map<Destination, Boolean> result = new EnumMap<>(Destination.class);
    // If user set destinations, enable these destinations and disable others
    if (destinations.length > 0) {
      result.put(Destination.ALL, false);
      for (Destination dest : destinations) {
        result.put(dest, true);
      }
    }
    return result;
  }
}

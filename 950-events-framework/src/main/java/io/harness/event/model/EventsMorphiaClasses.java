/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.model;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class EventsMorphiaClasses {
  public static final Set<Class> classes = ImmutableSet.<Class>of(GenericEvent.class);
}

package io.harness.event.model;

import com.google.common.collect.ImmutableSet;

import lombok.experimental.UtilityClass;

import java.util.Set;

@UtilityClass
public class EventsMorphiaClasses {
  public static final Set<Class> classes = ImmutableSet.<Class>of(GenericEvent.class);
}

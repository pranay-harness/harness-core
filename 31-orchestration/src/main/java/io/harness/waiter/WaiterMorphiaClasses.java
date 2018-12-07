package io.harness.waiter;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class WaiterMorphiaClasses {
  // TODO: this is temporarily listing all the classes in the manager.
  //       Step by step this should be split in different dedicated sections

  public static final Set<Class> classes = ImmutableSet.<Class>of(
      WaitQueue.class, NotifyResponse.class, NotifyEvent.class, WaitInstance.class, WaitInstanceError.class);
}

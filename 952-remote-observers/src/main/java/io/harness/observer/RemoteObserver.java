package io.harness.observer;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class RemoteObserver {
  Class<?> subjectCLass;
  Class<?> observerClass;
  @Singular private List<Class> observers;
}

package io.harness.observer;

import java.lang.reflect.Method;

public class NoOpRemoteObserverInformerImpl implements RemoteObserverInformer {
  @Override
  public void sendEvent(Method method, Class<?> subjectClazz, Object... params) {
    // do nothing
  }
}

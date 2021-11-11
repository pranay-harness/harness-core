package io.harness.observer;

import java.lang.reflect.Method;

public interface RemoteObserverInformer {
  void sendEvent(Method method, Class<?> subjectClazz, Object... params);
}

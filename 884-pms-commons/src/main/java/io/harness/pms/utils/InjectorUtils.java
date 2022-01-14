package io.harness.pms.utils;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class InjectorUtils {
  @Inject Injector injector;
  public void injectMembers(List<?> objs) {
    objs.forEach(obj -> injector.injectMembers(obj));
  }
}

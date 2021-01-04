package software.wings.app;

import software.wings.licensing.LicenseInterceptor;
import software.wings.licensing.Licensed;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;

/**
 * Created by peeyushaggarwal on 3/22/17.
 */
public class LicenseModule extends AbstractModule {
  @Override
  protected void configure() {
    LicenseInterceptor interceptor = new LicenseInterceptor();
    requestInjection(interceptor);
    bind(LicenseInterceptor.class).toInstance(interceptor);
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(Licensed.class), interceptor);
    bindInterceptor(Matchers.annotatedWith(Licensed.class), Matchers.any(), interceptor);
  }
}

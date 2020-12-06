package software.wings.delegatetasks;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.SyncTaskContext;
import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.lang.reflect.Proxy;

/**
 * Created by peeyushaggarwal on 1/12/17.
 */
@Singleton
@TargetModule(Module._930_DELEGATE_TASKS)
public class DelegateProxyFactory {
  @Inject private DelegateService delegateService;

  public <T> T get(Class<T> klass, SyncTaskContext syncTaskContext) {
    return (T) Proxy.newProxyInstance(
        klass.getClassLoader(), new Class[] {klass}, new DelegateInvocationHandler(syncTaskContext, delegateService));
  }
}

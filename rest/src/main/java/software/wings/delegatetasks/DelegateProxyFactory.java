package software.wings.delegatetasks;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.service.intfc.DelegateService;

import java.lang.reflect.Proxy;

/**
 * Created by peeyushaggarwal on 1/12/17.
 */
@Singleton
public class DelegateProxyFactory {
  @Inject private DelegateService delegateService;

  public <T> T get(Class<T> klass, SyncTaskContext syncTaskContext) {
    return (T) Proxy.newProxyInstance(
        klass.getClassLoader(), new Class[] {klass}, new DelegateInvocationHandler(syncTaskContext, delegateService));
  }
}

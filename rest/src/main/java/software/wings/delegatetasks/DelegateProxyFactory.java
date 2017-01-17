package software.wings.delegatetasks;

import com.google.inject.Singleton;

import software.wings.beans.DelegateTask.Context;
import software.wings.service.intfc.DelegateService;

import java.lang.reflect.Proxy;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 1/12/17.
 */
@Singleton
public class DelegateProxyFactory {
  @Inject private DelegateService delegateService;

  public <T> T get(Class<T> klass, Context context) {
    return (T) Proxy.newProxyInstance(
        klass.getClassLoader(), new Class[] {klass}, new DelegateInvocationHandler(context, delegateService));
  }
}

package software.wings.delegatetasks;

import static org.joor.Reflect.on;
import static software.wings.delegatetasks.RemoteMethodReturnValueData.Builder.aRemoteMethodReturnValueData;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;

import org.joor.ReflectException;
import software.wings.beans.DelegateTask;

import java.util.function.Consumer;

/**
 * Created by peeyushaggarwal on 1/12/17.
 */
public class ServiceImplDelegateTask extends AbstractDelegateRunnableTask<RemoteMethodReturnValueData> {
  @Inject private Injector injector;

  public ServiceImplDelegateTask(
      String delegateId, DelegateTask delegateTask, Consumer<RemoteMethodReturnValueData> consumer) {
    super(delegateId, delegateTask, consumer);
  }

  @Override
  public RemoteMethodReturnValueData run(Object[] parameters) {
    String key = (String) parameters[0];
    Object service = null;
    try {
      service = injector.getInstance(Key.get(Class.forName(key)));
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
    String method = (String) parameters[1];
    Throwable exception = null;
    Object methodReturnValue = null;
    try {
      if (parameters.length == 2) {
        methodReturnValue = on(service).call(method).get();
      } else {
        Object[] methodParameters = new Object[parameters.length - 2];
        System.arraycopy(parameters, 2, methodParameters, 0, parameters.length - 2);
        methodReturnValue = on(service).call(method, methodParameters).get();
      }
    } catch (ReflectException e) {
      if (e.getCause().getClass().isAssignableFrom(NoSuchMethodException.class)) {
        exception = e.getCause();
      } else {
        exception = e.getCause();
        if (exception.getCause() != null) {
          exception = exception.getCause();
        }
      }
    }
    return aRemoteMethodReturnValueData().withReturnValue(methodReturnValue).withException(exception).build();
  }
}

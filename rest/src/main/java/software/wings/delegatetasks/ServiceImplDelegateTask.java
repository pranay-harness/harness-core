package software.wings.delegatetasks;

import static io.harness.exception.WingsException.ExecutionContext.DELEGATE;
import static org.joor.Reflect.on;
import static software.wings.delegatetasks.RemoteMethodReturnValueData.Builder.aRemoteMethodReturnValueData;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;

import io.harness.exception.WingsException;
import org.joor.ReflectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.exception.WingsExceptionMapper;
import software.wings.waitnotify.NotifyResponseData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by peeyushaggarwal on 1/12/17.
 */
public class ServiceImplDelegateTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(AbstractDelegateRunnableTask.class);

  @Inject private Injector injector;

  public ServiceImplDelegateTask(String delegateId, DelegateTask delegateTask, Consumer<NotifyResponseData> postExecute,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, postExecute, preExecute);
  }

  @Override
  public RemoteMethodReturnValueData run(Object[] parameters) {
    String key = (String) parameters[0];
    Object service = null;
    try {
      service = injector.getInstance(Key.get(Class.forName(key)));
    } catch (ClassNotFoundException e) {
      logger.error("", e);
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
      if (exception instanceof WingsException) {
        WingsExceptionMapper.logProcessedMessages((WingsException) exception, DELEGATE, logger);
      } else {
        logger.error("Task error", exception);
      }
    }
    return aRemoteMethodReturnValueData().withReturnValue(methodReturnValue).withException(exception).build();
  }
}

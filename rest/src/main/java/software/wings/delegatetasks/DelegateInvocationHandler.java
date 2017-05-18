package software.wings.delegatetasks;

import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import software.wings.beans.DelegateTask.Context;
import software.wings.beans.TaskType;
import software.wings.service.intfc.DelegateService;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by peeyushaggarwal on 1/12/17.
 */
public class DelegateInvocationHandler implements InvocationHandler {
  private DelegateService delegateService;
  private Context context;

  public DelegateInvocationHandler(Context context, DelegateService delegateService) {
    this.delegateService = delegateService;
    this.context = context;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    TaskType taskType = method.getAnnotation(DelegateTaskType.class).value();
    Object[] delegateArguments = new Object[args.length + 2];
    delegateArguments[0] = proxy.getClass().getInterfaces()[0].getName();
    delegateArguments[1] = method.getName();
    System.arraycopy(args, 0, delegateArguments, 2, args.length);
    RemoteMethodReturnValueData returnValueData = delegateService.executeTask(aDelegateTask()
                                                                                  .withTaskType(taskType)
                                                                                  .withParameters(delegateArguments)
                                                                                  .withAccountId(context.getAccountId())
                                                                                  .withAppId(context.getAppId())
                                                                                  .build(),
        context.getTimeOut());
    if (returnValueData.getException() != null) {
      throw returnValueData.getException();
    } else {
      return returnValueData.getReturnValue();
    }
  }
}

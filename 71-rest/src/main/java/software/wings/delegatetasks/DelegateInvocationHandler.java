package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.singletonList;

import software.wings.beans.AwsConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTask.DelegateTaskBuilder;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.TaskType;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.intfc.DelegateService;
import software.wings.settings.SettingValue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by peeyushaggarwal on 1/12/17.
 */
public class DelegateInvocationHandler implements InvocationHandler {
  private DelegateService delegateService;
  private SyncTaskContext syncTaskContext;

  public DelegateInvocationHandler(SyncTaskContext syncTaskContext, DelegateService delegateService) {
    this.delegateService = delegateService;
    this.syncTaskContext = syncTaskContext;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    TaskType taskType = method.getAnnotation(DelegateTaskType.class).value();
    Object[] delegateArguments = new Object[args.length + 2];
    delegateArguments[0] = proxy.getClass().getInterfaces()[0].getName();
    delegateArguments[1] = method.getName();
    System.arraycopy(args, 0, delegateArguments, 2, args.length);
    DelegateTaskBuilder builder = DelegateTask.builder()
                                      .taskType(taskType.name())
                                      .parameters(delegateArguments)
                                      .accountId(syncTaskContext.getAccountId())
                                      .appId(syncTaskContext.getAppId())
                                      .envId(syncTaskContext.getEnvId())
                                      .infrastructureMappingId(syncTaskContext.getInfrastructureMappingId())
                                      .async(false)
                                      .timeout(syncTaskContext.getTimeout())
                                      .tags(syncTaskContext.getTags())
                                      .correlationId(syncTaskContext.getCorrelationId());

    String awsConfigTag = getAwsConfigTags(args);
    if (isNotEmpty(awsConfigTag)) {
      builder.tags(singletonList(awsConfigTag));
    }
    RemoteMethodReturnValueData returnValueData = delegateService.executeTask(builder.build());
    if (returnValueData.getException() != null) {
      throw returnValueData.getException();
    } else {
      return returnValueData.getReturnValue();
    }
  }

  private String getAwsConfigTags(Object[] args) {
    if (isNotEmpty(args)) {
      for (Object arg : args) {
        if (arg instanceof AwsConfig) {
          return ((AwsConfig) arg).getTag();
        } else if (arg instanceof SettingAttribute) {
          SettingValue settingValue = ((SettingAttribute) arg).getValue();
          if (settingValue instanceof AwsConfig) {
            return ((AwsConfig) settingValue).getTag();
          }
        } else if (arg instanceof ContainerServiceParams) {
          SettingAttribute settingAttribute = ((ContainerServiceParams) arg).getSettingAttribute();
          SettingValue settingValue = settingAttribute.getValue();
          if (settingValue instanceof AwsConfig) {
            return ((AwsConfig) settingValue).getTag();
          }
        }
      }
    }
    return null;
  }
}

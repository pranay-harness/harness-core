package software.wings.beans;

import static org.joor.Reflect.on;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.DelegateRunnableTask;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Singleton
@OwnedBy(HarnessTeam.DEL)
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class DelegateTaskFactory {
  @Inject Map<TaskType, Class<? extends DelegateRunnableTask>> classMap;

  public DelegateRunnableTask getDelegateRunnableTask(TaskType type, DelegateTaskPackage delegateTaskPackage,
      ILogStreamingTaskClient logStreamingTaskClient, Consumer<DelegateTaskResponse> postExecute,
      BooleanSupplier preExecute) {
    return on(classMap.get(type)).create(delegateTaskPackage, logStreamingTaskClient, postExecute, preExecute).get();
  }
}

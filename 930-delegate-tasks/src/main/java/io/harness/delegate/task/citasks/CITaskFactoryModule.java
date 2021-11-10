package io.harness.delegate.task.citasks;

import io.harness.delegate.task.citasks.awsvm.CIAwsVmCleanupTaskHandler;
import io.harness.delegate.task.citasks.awsvm.CIAwsVmExecuteStepTaskHandler;
import io.harness.delegate.task.citasks.awsvm.CIAwsVmInitializeTaskHandler;
import io.harness.delegate.task.citasks.cik8handler.CIK8CleanupTaskHandler;
import io.harness.delegate.task.citasks.cik8handler.CIK8ExecuteStepTaskHandler;
import io.harness.delegate.task.citasks.cik8handler.CIK8InitializeTaskHandler;
import io.harness.delegate.task.citasks.cik8handler.K8ExecuteCommandTaskHandler;
import io.harness.threading.Sleeper;
import io.harness.threading.ThreadSleeper;
import io.harness.time.ClockTimer;
import io.harness.time.Timer;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class CITaskFactoryModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(ExecuteCommandTaskHandler.class).to(K8ExecuteCommandTaskHandler.class);
    bind(Sleeper.class).to(ThreadSleeper.class);
    bind(Timer.class).to(ClockTimer.class);

    bind(CIInitializeTaskHandler.class)
        .annotatedWith(Names.named(CITaskConstants.INIT_AWS_VM))
        .to(CIAwsVmInitializeTaskHandler.class);
    bind(CIInitializeTaskHandler.class)
        .annotatedWith(Names.named(CITaskConstants.INIT_K8))
        .to(CIK8InitializeTaskHandler.class);

    bind(CIExecuteStepTaskHandler.class)
        .annotatedWith(Names.named(CITaskConstants.EXECUTE_STEP_AWS_VM))
        .to(CIAwsVmExecuteStepTaskHandler.class);
    bind(CIExecuteStepTaskHandler.class)
        .annotatedWith(Names.named(CITaskConstants.EXECUTE_STEP_K8))
        .to(CIK8ExecuteStepTaskHandler.class);

    bind(CICleanupTaskHandler.class)
        .annotatedWith(Names.named(CITaskConstants.CLEANUP_AWS_VM))
        .to(CIAwsVmCleanupTaskHandler.class);
    bind(CICleanupTaskHandler.class)
        .annotatedWith(Names.named(CITaskConstants.CLEANUP_K8))
        .to(CIK8CleanupTaskHandler.class);
  }
}

package software.wings.delegatetasks.citasks;

import com.google.inject.AbstractModule;

import software.wings.delegatetasks.citasks.cik8handler.CIK8BuildTaskHandler;
import software.wings.delegatetasks.citasks.cik8handler.CIK8CleanupTaskHandler;
import software.wings.delegatetasks.citasks.cik8handler.ExecCommandListener;
import software.wings.delegatetasks.citasks.cik8handler.K8ExecCommandListener;
import software.wings.delegatetasks.citasks.cik8handler.K8ExecuteCommandTaskHandler;

public class CITaskFactoryModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(CIBuildTaskHandler.class).to(CIK8BuildTaskHandler.class);
    bind(ExecuteCommandTaskHandler.class).to(K8ExecuteCommandTaskHandler.class);
    bind(ExecCommandListener.class).to(K8ExecCommandListener.class);
    bind(CICleanupTaskHandler.class).to(CIK8CleanupTaskHandler.class);
  }
}

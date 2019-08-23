package io.harness.grpc;

import com.google.inject.multibindings.Multibinder;

import io.grpc.BindableService;
import io.harness.govern.DependencyModule;
import io.harness.perpetualtask.grpc.PerpetualTaskServiceGrpc;

import java.util.Set;

public class GrpcServerModule extends DependencyModule {
  private static volatile GrpcServerModule instance;

  public static GrpcServerModule getInstance() {
    if (instance == null) {
      instance = new GrpcServerModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    // TODO: make GrpcServerModule generic by removing PerpetualTaskService as a dependency

    Multibinder<BindableService> multiBinder = Multibinder.newSetBinder(binder(), BindableService.class);
    multiBinder.addBinding().to(PerpetualTaskServiceGrpc.class);
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return null;
  }
}

package io.harness.perpetualtask;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.harness.perpetualtask.example.SamplePerpetualTaskFactory;
import io.harness.perpetualtask.example.SamplePerpetualTaskParams;
import io.harness.perpetualtask.k8s.watch.K8sWatchTask;
import io.harness.perpetualtask.k8s.watch.K8sWatchTaskFactory;
import io.harness.perpetualtask.k8s.watch.K8sWatchTaskParams;

public class PerpetualTaskWorkerModule extends AbstractModule {
  @Override
  protected void configure() {
    install(
        new FactoryModuleBuilder().implement(PerpetualTask.class, K8sWatchTask.class).build(K8sWatchTaskFactory.class));

    MapBinder<String, PerpetualTaskFactory> mapBinder =
        MapBinder.newMapBinder(binder(), String.class, PerpetualTaskFactory.class);
    mapBinder.addBinding(SamplePerpetualTaskParams.class.getSimpleName()).to(SamplePerpetualTaskFactory.class);
    mapBinder.addBinding(K8sWatchTaskParams.class.getSimpleName()).to(K8sWatchTaskFactory.class);
    //
  }

  @Provides
  public ManagedChannel getGrpcChannel() {
    // TODO: move gRPC channel out of PerpetualTaskWorker
    // TODO: remove hardcoded gRPC port number
    return ManagedChannelBuilder.forAddress("localhost", 9879).usePlaintext().build();
  }
}

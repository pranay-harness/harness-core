package io.harness.perpetualtask;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.harness.k8s.model.Kind;
import io.harness.perpetualtask.PerpetualTaskServiceGrpc.PerpetualTaskServiceBlockingStub;
import io.harness.perpetualtask.ecs.EcsPerpetualTaskExecutor;
import io.harness.perpetualtask.ecs.EcsPerpetualTaskParams;
import io.harness.perpetualtask.example.SamplePerpetualTaskExecutor;
import io.harness.perpetualtask.example.SamplePerpetualTaskParams;
import io.harness.perpetualtask.k8s.watch.K8SWatchTaskExecutor;
import io.harness.perpetualtask.k8s.watch.K8sWatchTaskParams;
import io.harness.perpetualtask.k8s.watch.NodeWatcher;
import io.harness.perpetualtask.k8s.watch.Owner;
import io.harness.perpetualtask.k8s.watch.PodWatcher;
import io.harness.perpetualtask.k8s.watch.WatcherFactory;
import io.harness.perpetualtask.k8s.watch.functions.JobOwnerMappingFunction;
import io.harness.perpetualtask.k8s.watch.functions.PodOwnerMappingFunction;
import io.harness.perpetualtask.k8s.watch.functions.ReplicaSetOwnerMappingFunction;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PerpetualTaskWorkerModule extends AbstractModule {
  public PerpetualTaskWorkerModule() {}

  @Override
  protected void configure() {
    MapBinder<String, PerpetualTaskExecutor> mapBinder =
        MapBinder.newMapBinder(binder(), String.class, PerpetualTaskExecutor.class);
    mapBinder.addBinding(SamplePerpetualTaskParams.class.getSimpleName()).to(SamplePerpetualTaskExecutor.class);
    mapBinder.addBinding(K8sWatchTaskParams.class.getSimpleName()).to(K8SWatchTaskExecutor.class);
    mapBinder.addBinding(EcsPerpetualTaskParams.class.getSimpleName()).to(EcsPerpetualTaskExecutor.class);

    install(new FactoryModuleBuilder()
                .implement(PodWatcher.class, PodWatcher.class)
                .implement(NodeWatcher.class, NodeWatcher.class)
                .build(WatcherFactory.class));

    MapBinder<String, PodOwnerMappingFunction<OwnerReference, KubernetesClient, String, Owner>>
        workloadKindToNameFunctionMap = MapBinder.newMapBinder(binder(), new TypeLiteral<String>() {},
            new TypeLiteral<PodOwnerMappingFunction<OwnerReference, KubernetesClient, String, Owner>>() {});
    workloadKindToNameFunctionMap.addBinding(Kind.Job.name()).toInstance(new JobOwnerMappingFunction());
    workloadKindToNameFunctionMap.addBinding(Kind.ReplicaSet.name()).toInstance(new ReplicaSetOwnerMappingFunction());
  }

  @Provides
  @Singleton
  PerpetualTaskServiceBlockingStub perpetualTaskServiceBlockingStub(
      @Named("manager-channel") Channel channel, CallCredentials callCredentials) {
    return PerpetualTaskServiceGrpc.newBlockingStub(channel).withCallCredentials(callCredentials);
  }
}

package software.wings.app;

import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;

import graphql.GraphQL;
import org.dataloader.MappedBatchLoader;
import org.jetbrains.annotations.NotNull;
import software.wings.beans.infrastructure.instance.info.AutoScalingGroupInstanceInfo;
import software.wings.beans.infrastructure.instance.info.CodeDeployInstanceInfo;
import software.wings.beans.infrastructure.instance.info.Ec2InstanceInfo;
import software.wings.beans.infrastructure.instance.info.EcsContainerInfo;
import software.wings.beans.infrastructure.instance.info.K8sPodInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.beans.infrastructure.instance.info.PcfInstanceInfo;
import software.wings.beans.infrastructure.instance.info.PhysicalHostInstanceInfo;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.datafetcher.DataLoaderRegistryHelper;
import software.wings.graphql.datafetcher.application.ApplicationConnectionDataFetcher;
import software.wings.graphql.datafetcher.application.ApplicationDataFetcher;
import software.wings.graphql.datafetcher.environment.EnvironmentConnectionDataFetcher;
import software.wings.graphql.datafetcher.environment.EnvironmentDataFetcher;
import software.wings.graphql.datafetcher.execution.ExecutionConnectionDataFetcher;
import software.wings.graphql.datafetcher.execution.ExecutionDataFetcher;
import software.wings.graphql.datafetcher.instance.InstanceConnectionDataFetcher;
import software.wings.graphql.datafetcher.instance.InstanceCountDataFetcher;
import software.wings.graphql.datafetcher.instance.instanceInfo.AutoScalingGroupInstanceInfoController;
import software.wings.graphql.datafetcher.instance.instanceInfo.CodeDeployInstanceInfoController;
import software.wings.graphql.datafetcher.instance.instanceInfo.Ec2InstanceInfoController;
import software.wings.graphql.datafetcher.instance.instanceInfo.EcsContainerInfoController;
import software.wings.graphql.datafetcher.instance.instanceInfo.InstanceInfoController;
import software.wings.graphql.datafetcher.instance.instanceInfo.K8sPodInfoController;
import software.wings.graphql.datafetcher.instance.instanceInfo.KubernetesContainerInfoController;
import software.wings.graphql.datafetcher.instance.instanceInfo.PcfInstanceInfoController;
import software.wings.graphql.datafetcher.instance.instanceInfo.PhysicalHostInstanceInfoController;
import software.wings.graphql.datafetcher.pipeline.PipelineConnectionDataFetcher;
import software.wings.graphql.datafetcher.pipeline.PipelineDataFetcher;
import software.wings.graphql.datafetcher.service.ServiceConnectionDataFetcher;
import software.wings.graphql.datafetcher.service.ServiceDataFetcher;
import software.wings.graphql.datafetcher.workflow.WorkflowConnectionDataFetcher;
import software.wings.graphql.datafetcher.workflow.WorkflowDataFetcher;
import software.wings.graphql.directive.DataFetcherDirective;
import software.wings.graphql.provider.GraphQLProvider;
import software.wings.graphql.provider.QueryLanguageProvider;

import java.util.Collections;
import java.util.Set;

/**
 * Created a new module as part of code review comment
 */
public class GraphQLModule extends AbstractModule {
  /***
   * This collection is mainly required to inject batched loader at app start time.
   * I was not getting a handle to Annotation Name at runtime hence I am taking this approach.
   */
  private static final Set<String> BATCH_DATA_LOADER_NAMES = Sets.newHashSet();

  public static Set<String> getBatchDataLoaderNames() {
    return Collections.unmodifiableSet(BATCH_DATA_LOADER_NAMES);
  }

  @Override
  protected void configure() {
    bind(new TypeLiteral<QueryLanguageProvider<GraphQL>>() {}).to(GraphQLProvider.class).asEagerSingleton();
    bind(DataFetcherDirective.class).asEagerSingleton();
    bind(DataLoaderRegistryHelper.class).asEagerSingleton();
    bind(DataFetcherDirective.class).in(Scopes.SINGLETON);
    bind(DataLoaderRegistryHelper.class).in(Scopes.SINGLETON);

    // DATA FETCHERS ARE NOT SINGLETON AS THEY CAN HAVE DIFFERENT CONTEXT MAP
    bindDataFetchers();

    bindInstanceInfoControllers();
  }

  private void bindInstanceInfoControllers() {
    MapBinder<Class, InstanceInfoController> instanceInfoControllerMapBinder =
        MapBinder.newMapBinder(binder(), Class.class, InstanceInfoController.class);
    instanceInfoControllerMapBinder.addBinding(PhysicalHostInstanceInfo.class)
        .to(PhysicalHostInstanceInfoController.class);
    instanceInfoControllerMapBinder.addBinding(AutoScalingGroupInstanceInfo.class)
        .to(AutoScalingGroupInstanceInfoController.class);
    instanceInfoControllerMapBinder.addBinding(Ec2InstanceInfo.class).to(Ec2InstanceInfoController.class);
    instanceInfoControllerMapBinder.addBinding(CodeDeployInstanceInfo.class).to(CodeDeployInstanceInfoController.class);
    instanceInfoControllerMapBinder.addBinding(EcsContainerInfo.class).to(EcsContainerInfoController.class);
    instanceInfoControllerMapBinder.addBinding(K8sPodInfo.class).to(K8sPodInfoController.class);
    instanceInfoControllerMapBinder.addBinding(KubernetesContainerInfo.class)
        .to(KubernetesContainerInfoController.class);
    instanceInfoControllerMapBinder.addBinding(PcfInstanceInfo.class).to(PcfInstanceInfoController.class);
  }

  private void bindDataFetchers() {
    bindDataFetcherWithAnnotation(ApplicationConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(ApplicationDataFetcher.class);
    bindDataFetcherWithAnnotation(EnvironmentConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(EnvironmentDataFetcher.class);
    bindDataFetcherWithAnnotation(ExecutionConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(ExecutionDataFetcher.class);
    bindDataFetcherWithAnnotation(InstanceCountDataFetcher.class);
    bindDataFetcherWithAnnotation(InstanceConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(PipelineConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(PipelineDataFetcher.class);
    bindDataFetcherWithAnnotation(ServiceConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(ServiceDataFetcher.class);
    bindDataFetcherWithAnnotation(WorkflowConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(WorkflowDataFetcher.class);
  }

  @NotNull
  private String calculateAnnotationName(Class clazz, String suffixToRemove) {
    String className = clazz.getName();
    char c[] = className.substring(className.lastIndexOf('.') + 1, clazz.getName().length() - suffixToRemove.length())
                   .toCharArray();

    c[0] = Character.toLowerCase(c[0]);
    return new String(c);
  }

  private void bindDataFetcherWithAnnotation(Class<? extends AbstractDataFetcher> clazz, String suffix) {
    String annotationName = calculateAnnotationName(clazz, "DataFetcher");
    bind(AbstractDataFetcher.class).annotatedWith(Names.named(annotationName)).to(clazz);
  }

  private void bindDataFetcherWithAnnotation(Class<? extends AbstractDataFetcher> clazz) {
    bindDataFetcherWithAnnotation(clazz, "");
  }

  private void bindBatchedDataLoaderWithAnnotation(Class<? extends MappedBatchLoader> clazz) {
    String annotationName = calculateAnnotationName(clazz, "BatchDataLoader");

    BATCH_DATA_LOADER_NAMES.add(annotationName);
    bind(MappedBatchLoader.class).annotatedWith(Names.named(annotationName)).to(clazz).in(Scopes.SINGLETON);
  }
}

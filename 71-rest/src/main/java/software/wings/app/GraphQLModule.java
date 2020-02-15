package software.wings.app;

import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;

import graphql.GraphQL;
import graphql.schema.DataFetcher;
import org.dataloader.MappedBatchLoader;
import org.hibernate.validator.constraints.NotBlank;
import org.jetbrains.annotations.NotNull;
import software.wings.beans.infrastructure.instance.info.AutoScalingGroupInstanceInfo;
import software.wings.beans.infrastructure.instance.info.CodeDeployInstanceInfo;
import software.wings.beans.infrastructure.instance.info.Ec2InstanceInfo;
import software.wings.beans.infrastructure.instance.info.EcsContainerInfo;
import software.wings.beans.infrastructure.instance.info.K8sPodInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.beans.infrastructure.instance.info.PcfInstanceInfo;
import software.wings.beans.infrastructure.instance.info.PhysicalHostInstanceInfo;
import software.wings.graphql.datafetcher.DataLoaderRegistryHelper;
import software.wings.graphql.datafetcher.application.ApplicationConnectionDataFetcher;
import software.wings.graphql.datafetcher.application.ApplicationDataFetcher;
import software.wings.graphql.datafetcher.application.ApplicationGitSyncConfigDataFetcher;
import software.wings.graphql.datafetcher.application.ApplicationStatsDataFetcher;
import software.wings.graphql.datafetcher.application.CreateApplicationDataFetcher;
import software.wings.graphql.datafetcher.application.DeleteApplicationDataFetcher;
import software.wings.graphql.datafetcher.application.RemoveApplicationGitSyncConfigDataFetcher;
import software.wings.graphql.datafetcher.application.UpdateApplicationDataFetcher;
import software.wings.graphql.datafetcher.application.UpdateApplicationGitSyncConfigDataFetcher;
import software.wings.graphql.datafetcher.application.UpdateApplicationGitSyncConfigStatusDataFetcher;
import software.wings.graphql.datafetcher.application.batch.ApplicationBatchDataFetcher;
import software.wings.graphql.datafetcher.application.batch.ApplicationBatchDataLoader;
import software.wings.graphql.datafetcher.artifact.ArtifactDataFetcher;
import software.wings.graphql.datafetcher.audit.ChangeContentConnectionDataFetcher;
import software.wings.graphql.datafetcher.audit.ChangeSetConnectionDataFetcher;
import software.wings.graphql.datafetcher.billing.BillingStatsEntityDataFetcher;
import software.wings.graphql.datafetcher.billing.BillingStatsFilterValuesDataFetcher;
import software.wings.graphql.datafetcher.billing.BillingStatsTimeSeriesDataFetcher;
import software.wings.graphql.datafetcher.billing.BillingTrendStatsDataFetcher;
import software.wings.graphql.datafetcher.billing.EfficiencyStatsDataFetcher;
import software.wings.graphql.datafetcher.billing.IdleCostTrendStatsDataFetcher;
import software.wings.graphql.datafetcher.billing.SunburstChartStatsDataFetcher;
import software.wings.graphql.datafetcher.budget.BudgetDataFetcher;
import software.wings.graphql.datafetcher.budget.BudgetListDataFetcher;
import software.wings.graphql.datafetcher.budget.BudgetTrendStatsDataFetcher;
import software.wings.graphql.datafetcher.cloudProvider.CloudProviderConnectionDataFetcher;
import software.wings.graphql.datafetcher.cloudProvider.CloudProviderDataFetcher;
import software.wings.graphql.datafetcher.cloudProvider.CloudProviderStatsDataFetcher;
import software.wings.graphql.datafetcher.cloudefficiencyevents.EventsStatsDataFetcher;
import software.wings.graphql.datafetcher.cluster.ClusterConnectionDataFetcher;
import software.wings.graphql.datafetcher.cluster.ClusterDataFetcher;
import software.wings.graphql.datafetcher.connector.ConnectorConnectionDataFetcher;
import software.wings.graphql.datafetcher.connector.ConnectorDataFetcher;
import software.wings.graphql.datafetcher.connector.ConnectorStatsDataFetcher;
import software.wings.graphql.datafetcher.environment.EnvironmentConnectionDataFetcher;
import software.wings.graphql.datafetcher.environment.EnvironmentDataFetcher;
import software.wings.graphql.datafetcher.environment.EnvironmentStatsDataFetcher;
import software.wings.graphql.datafetcher.environment.batch.EnvironmentBatchDataFetcher;
import software.wings.graphql.datafetcher.environment.batch.EnvironmentBatchDataLoader;
import software.wings.graphql.datafetcher.execution.DeploymentStatsDataFetcher;
import software.wings.graphql.datafetcher.execution.ExecutionConnectionDataFetcher;
import software.wings.graphql.datafetcher.execution.ExecutionDataFetcher;
import software.wings.graphql.datafetcher.instance.InstanceConnectionDataFetcher;
import software.wings.graphql.datafetcher.instance.InstanceCountDataFetcher;
import software.wings.graphql.datafetcher.instance.InstanceStatsDataFetcher;
import software.wings.graphql.datafetcher.instance.instanceInfo.AutoScalingGroupInstanceController;
import software.wings.graphql.datafetcher.instance.instanceInfo.CodeDeployInstanceController;
import software.wings.graphql.datafetcher.instance.instanceInfo.Ec2InstanceController;
import software.wings.graphql.datafetcher.instance.instanceInfo.EcsContainerController;
import software.wings.graphql.datafetcher.instance.instanceInfo.InstanceController;
import software.wings.graphql.datafetcher.instance.instanceInfo.K8SPodController;
import software.wings.graphql.datafetcher.instance.instanceInfo.KubernetesContainerController;
import software.wings.graphql.datafetcher.instance.instanceInfo.PcfInstanceController;
import software.wings.graphql.datafetcher.instance.instanceInfo.PhysicalHostInstanceController;
import software.wings.graphql.datafetcher.k8sLabel.K8sLabelConnectionDataFetcher;
import software.wings.graphql.datafetcher.outcome.OutcomeConnectionDataFetcher;
import software.wings.graphql.datafetcher.pipeline.PipelineConnectionDataFetcher;
import software.wings.graphql.datafetcher.pipeline.PipelineDataFetcher;
import software.wings.graphql.datafetcher.pipeline.PipelineStatsDataFetcher;
import software.wings.graphql.datafetcher.secrets.CreateSecretDataFetcher;
import software.wings.graphql.datafetcher.secrets.UpdateSecretDataFetcher;
import software.wings.graphql.datafetcher.service.ServiceConnectionDataFetcher;
import software.wings.graphql.datafetcher.service.ServiceDataFetcher;
import software.wings.graphql.datafetcher.service.ServiceStatsDataFetcher;
import software.wings.graphql.datafetcher.service.batch.ServiceBatchDataFetcher;
import software.wings.graphql.datafetcher.service.batch.ServiceBatchDataLoader;
import software.wings.graphql.datafetcher.ssoProvider.SsoProviderConnectionDataFetcher;
import software.wings.graphql.datafetcher.ssoProvider.SsoProviderDataFetcher;
import software.wings.graphql.datafetcher.tag.TagsDataFetcher;
import software.wings.graphql.datafetcher.tag.TagsInUseConnectionDataFetcher;
import software.wings.graphql.datafetcher.trigger.TriggerConnectionDataFetcher;
import software.wings.graphql.datafetcher.trigger.TriggerDataFetcher;
import software.wings.graphql.datafetcher.trigger.TriggerStatsDataFetcher;
import software.wings.graphql.datafetcher.user.CreateUserDataFetcher;
import software.wings.graphql.datafetcher.user.DeleteUserDataFetcher;
import software.wings.graphql.datafetcher.user.UpdateUserDataFetcher;
import software.wings.graphql.datafetcher.user.UserConnectionDataFetcher;
import software.wings.graphql.datafetcher.user.UserDataFetcher;
import software.wings.graphql.datafetcher.userGroup.CreateUserGroupDataFetcher;
import software.wings.graphql.datafetcher.userGroup.DeleteUserGroupDataFetcher;
import software.wings.graphql.datafetcher.userGroup.UpdateUserGroupDataFetcher;
import software.wings.graphql.datafetcher.userGroup.UpdateUserGroupPermissionsDataFetcher;
import software.wings.graphql.datafetcher.userGroup.UserGroupConnectionDataFetcher;
import software.wings.graphql.datafetcher.userGroup.UserGroupDataFetcher;
import software.wings.graphql.datafetcher.workflow.WorkflowConnectionDataFetcher;
import software.wings.graphql.datafetcher.workflow.WorkflowDataFetcher;
import software.wings.graphql.datafetcher.workflow.WorkflowStatsDataFetcher;
import software.wings.graphql.directive.DataFetcherDirective;
import software.wings.graphql.instrumentation.QLAuditInstrumentation;
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
  private static final String DATA_FETCHER_SUFFIX = "DataFetcher";
  public static final String BATCH_SUFFIX = "Batch";
  private static final String BATCH_DATA_LOADER_SUFFIX = BATCH_SUFFIX.concat("DataLoader");

  public static Set<String> getBatchDataLoaderNames() {
    return Collections.unmodifiableSet(BATCH_DATA_LOADER_NAMES);
  }

  @Override
  protected void configure() {
    bind(new TypeLiteral<QueryLanguageProvider<GraphQL>>() {}).to(GraphQLProvider.class).asEagerSingleton();
    bind(DataFetcherDirective.class).asEagerSingleton();
    bind(DataLoaderRegistryHelper.class).asEagerSingleton();
    bind(QLAuditInstrumentation.class).asEagerSingleton();
    bind(DataFetcherDirective.class).in(Scopes.SINGLETON);
    bind(DataLoaderRegistryHelper.class).in(Scopes.SINGLETON);
    bind(QLAuditInstrumentation.class).in(Scopes.SINGLETON);

    // DATA FETCHERS ARE NOT SINGLETON AS THEY CAN HAVE DIFFERENT CONTEXT MAP
    bindDataFetchers();

    bindBatchedDataLoaderWithAnnotation();

    bindInstanceInfoControllers();
  }

  private void bindBatchedDataLoaderWithAnnotation() {
    bindBatchedDataLoaderWithAnnotation(ApplicationBatchDataLoader.class);
    bindBatchedDataLoaderWithAnnotation(ServiceBatchDataLoader.class);
    bindBatchedDataLoaderWithAnnotation(EnvironmentBatchDataLoader.class);
  }

  private void bindInstanceInfoControllers() {
    MapBinder<Class, InstanceController> instanceInfoControllerMapBinder =
        MapBinder.newMapBinder(binder(), Class.class, InstanceController.class);
    instanceInfoControllerMapBinder.addBinding(PhysicalHostInstanceInfo.class).to(PhysicalHostInstanceController.class);
    instanceInfoControllerMapBinder.addBinding(AutoScalingGroupInstanceInfo.class)
        .to(AutoScalingGroupInstanceController.class);
    instanceInfoControllerMapBinder.addBinding(Ec2InstanceInfo.class).to(Ec2InstanceController.class);
    instanceInfoControllerMapBinder.addBinding(CodeDeployInstanceInfo.class).to(CodeDeployInstanceController.class);
    instanceInfoControllerMapBinder.addBinding(EcsContainerInfo.class).to(EcsContainerController.class);
    instanceInfoControllerMapBinder.addBinding(K8sPodInfo.class).to(K8SPodController.class);
    instanceInfoControllerMapBinder.addBinding(KubernetesContainerInfo.class).to(KubernetesContainerController.class);
    instanceInfoControllerMapBinder.addBinding(PcfInstanceInfo.class).to(PcfInstanceController.class);
  }

  private void bindDataFetchers() {
    bindDataFetcherWithAnnotation(ApplicationBatchDataFetcher.class);
    bindDataFetcherWithAnnotation(ApplicationConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(ApplicationDataFetcher.class);
    bindDataFetcherWithAnnotation(ApplicationStatsDataFetcher.class);
    bindDataFetcherWithAnnotation(ArtifactDataFetcher.class);
    bindDataFetcherWithAnnotation(BillingStatsEntityDataFetcher.class);
    bindDataFetcherWithAnnotation(BillingStatsFilterValuesDataFetcher.class);
    bindDataFetcherWithAnnotation(BillingStatsTimeSeriesDataFetcher.class);
    bindDataFetcherWithAnnotation(BillingTrendStatsDataFetcher.class);
    bindDataFetcherWithAnnotation(BudgetDataFetcher.class);
    bindDataFetcherWithAnnotation(BudgetListDataFetcher.class);
    bindDataFetcherWithAnnotation(BudgetTrendStatsDataFetcher.class);
    bindDataFetcherWithAnnotation(CloudProviderConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(CloudProviderDataFetcher.class);
    bindDataFetcherWithAnnotation(ClusterDataFetcher.class);
    bindDataFetcherWithAnnotation(ClusterConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(ConnectorConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(ConnectorStatsDataFetcher.class);
    bindDataFetcherWithAnnotation(CloudProviderStatsDataFetcher.class);
    bindDataFetcherWithAnnotation(ConnectorDataFetcher.class);
    bindDataFetcherWithAnnotation(DeploymentStatsDataFetcher.class);
    bindDataFetcherWithAnnotation(EfficiencyStatsDataFetcher.class);
    bindDataFetcherWithAnnotation(EnvironmentConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(EnvironmentDataFetcher.class);
    bindDataFetcherWithAnnotation(EnvironmentBatchDataFetcher.class);
    bindDataFetcherWithAnnotation(EnvironmentStatsDataFetcher.class);
    bindDataFetcherWithAnnotation(ExecutionConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(ExecutionDataFetcher.class);
    bindDataFetcherWithAnnotation(IdleCostTrendStatsDataFetcher.class);
    bindDataFetcherWithAnnotation(InstanceConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(InstanceCountDataFetcher.class);
    bindDataFetcherWithAnnotation(InstanceStatsDataFetcher.class);
    bindDataFetcherWithAnnotation(K8sLabelConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(OutcomeConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(PipelineConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(PipelineDataFetcher.class);
    bindDataFetcherWithAnnotation(PipelineStatsDataFetcher.class);
    bindDataFetcherWithAnnotation(ServiceConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(ServiceBatchDataFetcher.class);
    bindDataFetcherWithAnnotation(ServiceDataFetcher.class);
    bindDataFetcherWithAnnotation(ServiceStatsDataFetcher.class);
    bindDataFetcherWithAnnotation(SunburstChartStatsDataFetcher.class);
    bindDataFetcherWithAnnotation(TagsInUseConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(TagsDataFetcher.class);
    bindDataFetcherWithAnnotation(TriggerConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(TriggerStatsDataFetcher.class);
    bindDataFetcherWithAnnotation(TriggerDataFetcher.class);
    bindDataFetcherWithAnnotation(WorkflowConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(WorkflowDataFetcher.class);
    bindDataFetcherWithAnnotation(WorkflowStatsDataFetcher.class);
    bindDataFetcherWithAnnotation(ChangeSetConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(ChangeContentConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(CreateApplicationDataFetcher.class);
    bindDataFetcherWithAnnotation(UpdateApplicationDataFetcher.class);
    bindDataFetcherWithAnnotation(DeleteApplicationDataFetcher.class);
    bindDataFetcherWithAnnotation(UpdateUserGroupPermissionsDataFetcher.class);
    bindDataFetcherWithAnnotation(CreateUserDataFetcher.class);
    bindDataFetcherWithAnnotation(UpdateUserDataFetcher.class);
    bindDataFetcherWithAnnotation(DeleteUserDataFetcher.class);
    bindDataFetcherWithAnnotation(UserGroupDataFetcher.class);
    bindDataFetcherWithAnnotation(UserGroupConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(DeleteUserGroupDataFetcher.class);
    bindDataFetcherWithAnnotation(CreateUserGroupDataFetcher.class);
    bindDataFetcherWithAnnotation(UpdateUserGroupDataFetcher.class);
    bindDataFetcherWithAnnotation(UserDataFetcher.class);
    bindDataFetcherWithAnnotation(UserConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(EventsStatsDataFetcher.class);
    bindDataFetcherWithAnnotation(UpdateApplicationGitSyncConfigDataFetcher.class);
    bindDataFetcherWithAnnotation(RemoveApplicationGitSyncConfigDataFetcher.class);
    bindDataFetcherWithAnnotation(UpdateApplicationGitSyncConfigStatusDataFetcher.class);
    bindDataFetcherWithAnnotation(ApplicationGitSyncConfigDataFetcher.class);
    bindDataFetcherWithAnnotation(SsoProviderDataFetcher.class);
    bindDataFetcherWithAnnotation(SsoProviderConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(CreateSecretDataFetcher.class);
    bindDataFetcherWithAnnotation(UpdateSecretDataFetcher.class);
  }

  @NotNull
  public static String calculateAnnotationName(final Class clazz, String suffixToRemove) {
    String className = clazz.getName();
    char c[] = className.substring(className.lastIndexOf('.') + 1, clazz.getName().length() - suffixToRemove.length())
                   .toCharArray();

    c[0] = Character.toLowerCase(c[0]);
    return new String(c);
  }

  private void bindDataFetcherWithAnnotation(Class<? extends DataFetcher> clazz, String suffix) {
    String annotationName = calculateAnnotationName(clazz, suffix);
    bind(DataFetcher.class).annotatedWith(Names.named(annotationName)).to(clazz);
  }

  private void bindDataFetcherWithAnnotation(Class<? extends DataFetcher> clazz) {
    bindDataFetcherWithAnnotation(clazz, DATA_FETCHER_SUFFIX);
  }

  private void bindBatchedDataLoaderWithAnnotation(Class<? extends MappedBatchLoader> clazz) {
    String annotationName = calculateAnnotationName(clazz, BATCH_DATA_LOADER_SUFFIX);
    BATCH_DATA_LOADER_NAMES.add(annotationName);
    bind(MappedBatchLoader.class).annotatedWith(Names.named(annotationName)).to(clazz).in(Scopes.SINGLETON);
  }

  public static String getBatchDataLoaderAnnotationName(@NotBlank String dataFetcherName) {
    return dataFetcherName.concat(BATCH_SUFFIX);
  }
}

package software.wings.app;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import graphql.GraphQL;
import org.dataloader.MappedBatchLoader;
import org.jetbrains.annotations.NotNull;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.datafetcher.DataLoaderRegistryHelper;
import software.wings.graphql.datafetcher.application.ApplicationConnectionDataFetcher;
import software.wings.graphql.datafetcher.application.ApplicationDataFetcher;
import software.wings.graphql.datafetcher.environment.EnvironmentConnectionDataFetcher;
import software.wings.graphql.datafetcher.environment.EnvironmentDataFetcher;
import software.wings.graphql.datafetcher.execution.ExecutionConnectionDataFetcher;
import software.wings.graphql.datafetcher.execution.ExecutionDataFetcher;
import software.wings.graphql.datafetcher.instance.InstanceCountDataFetcher;
import software.wings.graphql.datafetcher.instance.InstancesByEnvTypeDataFetcher;
import software.wings.graphql.datafetcher.instance.InstancesByEnvironmentDataFetcher;
import software.wings.graphql.datafetcher.instance.InstancesByServiceDataFetcher;
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
import java.util.Map;
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

  private Set<String> dataFetcherClassNames;

  private static final Map<String, Class<? extends AbstractDataFetcher>> ANNOTATION_TO_DATA_FETCHER_MAP =
      Maps.newHashMap();

  public GraphQLModule() {
    dataFetcherClassNames = Sets.newHashSet();
  }

  public static Class<? extends AbstractDataFetcher> getAbstractDataFetcher(String annotationName) {
    return ANNOTATION_TO_DATA_FETCHER_MAP.get(annotationName);
  }

  @Override
  protected void configure() {
    bind(new TypeLiteral<QueryLanguageProvider<GraphQL>>() {}).to(GraphQLProvider.class).in(Scopes.SINGLETON);

    bind(DataFetcherDirective.class).in(Scopes.SINGLETON);

    bind(DataLoaderRegistryHelper.class).in(Scopes.SINGLETON);

    // DATA FETCHERS ARE NOT SINGLETON AS THEY CAN HAVE DIFFERENT CONTEXT MAP
    bindDataFetchers();
  }

  private void bindDataFetchers() {
    bindDataFetcherWithAnnotation(ApplicationConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(ApplicationDataFetcher.class);
    bindDataFetcherWithAnnotation(EnvironmentConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(EnvironmentDataFetcher.class);
    bindDataFetcherWithAnnotation(ExecutionConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(ExecutionConnectionDataFetcher.class, "ForPipeline");
    bindDataFetcherWithAnnotation(ExecutionConnectionDataFetcher.class, "ForService");
    bindDataFetcherWithAnnotation(ExecutionConnectionDataFetcher.class, "ForWorkflow");
    bindDataFetcherWithAnnotation(ExecutionDataFetcher.class);
    bindDataFetcherWithAnnotation(InstanceCountDataFetcher.class);
    bindDataFetcherWithAnnotation(InstancesByEnvironmentDataFetcher.class);
    bindDataFetcherWithAnnotation(InstancesByEnvironmentDataFetcher.class);
    bindDataFetcherWithAnnotation(InstancesByEnvTypeDataFetcher.class);
    bindDataFetcherWithAnnotation(InstancesByServiceDataFetcher.class);
    bindDataFetcherWithAnnotation(PipelineConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(PipelineDataFetcher.class);
    bindDataFetcherWithAnnotation(ServiceConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(ServiceDataFetcher.class);
    bindDataFetcherWithAnnotation(WorkflowConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(WorkflowDataFetcher.class);
  }

  @NotNull
  private String calculateAnnotationName(Class clazz, String suffixToRemove, String suffixToAdd) {
    String className = clazz.getName();
    char c[] = (className.substring(className.lastIndexOf('.') + 1, clazz.getName().length() - suffixToRemove.length())
        + suffixToAdd)
                   .toCharArray();

    c[0] = Character.toLowerCase(c[0]);
    return new String(c);
  }

  private void bindDataFetcherWithAnnotation(Class<? extends AbstractDataFetcher> clazz, String suffix) {
    String canonicalName = clazz.getCanonicalName();
    if (!dataFetcherClassNames.contains(canonicalName)) {
      bind(clazz).in(Scopes.SINGLETON);
      dataFetcherClassNames.add(canonicalName);
    }
    String annotationName = calculateAnnotationName(clazz, "DataFetcher", suffix);
    ANNOTATION_TO_DATA_FETCHER_MAP.putIfAbsent(annotationName, clazz);
  }

  private void bindDataFetcherWithAnnotation(Class<? extends AbstractDataFetcher> clazz) {
    bindDataFetcherWithAnnotation(clazz, "");
  }

  private void bindBatchedDataLoaderWithAnnotation(Class<? extends MappedBatchLoader> clazz) {
    String annotationName = calculateAnnotationName(clazz, "BatchDataLoader", "");

    BATCH_DATA_LOADER_NAMES.add(annotationName);
    bind(MappedBatchLoader.class).annotatedWith(Names.named(annotationName)).to(clazz).in(Scopes.SINGLETON);
  }
}

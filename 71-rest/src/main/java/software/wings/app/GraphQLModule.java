package software.wings.app;

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
import software.wings.graphql.datafetcher.application.ApplicationDataFetcher;
import software.wings.graphql.datafetcher.application.ApplicationsDataFetcher;
import software.wings.graphql.datafetcher.application.batchloader.ApplicationBatchDataLoader;
import software.wings.graphql.datafetcher.artifact.ArtifactDataFetcher;
import software.wings.graphql.datafetcher.environment.EnvironmentDataFetcher;
import software.wings.graphql.datafetcher.environment.EnvironmentsDataFetcher;
import software.wings.graphql.datafetcher.pipeline.PipelineConnectionDataFetcher;
import software.wings.graphql.datafetcher.pipeline.PipelineDataFetcher;
import software.wings.graphql.datafetcher.service.ServiceDataFetcher;
import software.wings.graphql.datafetcher.workflow.WorkflowDataFetcher;
import software.wings.graphql.datafetcher.workflow.WorkflowExecutionDataFetcher;
import software.wings.graphql.datafetcher.workflow.WorkflowExecutionsDataFetcher;
import software.wings.graphql.datafetcher.workflow.WorkflowsDataFetcher;
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
    bind(new TypeLiteral<QueryLanguageProvider<GraphQL>>() {}).to(GraphQLProvider.class).in(Scopes.SINGLETON);

    bind(DataFetcherDirective.class).in(Scopes.SINGLETON);

    bind(DataLoaderRegistryHelper.class).in(Scopes.SINGLETON);

    // DATA FETCHERS ARE NOT SINGLETON AS THEY CAN HAVE DIFFERENT CONTEXT MAP
    bindDataFetchers();

    // Add all batched data fetchers are SINGLETON .
    bindBatchedDataLoaders();
  }

  private void bindDataFetchers() {
    bindDataFetcherWithAnnotation(PipelineDataFetcher.class);
    bindDataFetcherWithAnnotation(PipelineConnectionDataFetcher.class);
    bindDataFetcherWithAnnotation(WorkflowDataFetcher.class);
    bindDataFetcherWithAnnotation(WorkflowsDataFetcher.class);
    bindDataFetcherWithAnnotation(WorkflowExecutionDataFetcher.class);
    bindDataFetcherWithAnnotation(WorkflowExecutionsDataFetcher.class);
    bindDataFetcherWithAnnotation(ArtifactDataFetcher.class);
    bindDataFetcherWithAnnotation(ApplicationDataFetcher.class);
    bindDataFetcherWithAnnotation(EnvironmentDataFetcher.class);
    bindDataFetcherWithAnnotation(EnvironmentsDataFetcher.class);
    bindDataFetcherWithAnnotation(ApplicationsDataFetcher.class);
    bindDataFetcherWithAnnotation(ServiceDataFetcher.class);
  }

  @NotNull
  private String calculateAnnotationName(Class clazz, String suffix) {
    String className = clazz.getName();
    char c[] =
        className.substring(className.lastIndexOf('.') + 1, clazz.getName().length() - suffix.length()).toCharArray();

    c[0] = Character.toLowerCase(c[0]);
    return new String(c);
  }

  private void bindBatchedDataLoaders() {
    bindBatchedDataLoaderWithAnnotation(ApplicationBatchDataLoader.class);
  }

  private void bindDataFetcherWithAnnotation(Class<? extends AbstractDataFetcher> clazz) {
    String annotationName = calculateAnnotationName(clazz, "DataFetcher");
    bind(AbstractDataFetcher.class).annotatedWith(Names.named(annotationName)).to(clazz);
  }

  private void bindBatchedDataLoaderWithAnnotation(Class<? extends MappedBatchLoader> clazz) {
    String annotationName = calculateAnnotationName(clazz, "BatchDataLoader");

    BATCH_DATA_LOADER_NAMES.add(annotationName);
    bind(MappedBatchLoader.class).annotatedWith(Names.named(annotationName)).to(clazz).in(Scopes.SINGLETON);
  }
}

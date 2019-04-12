package software.wings.app;

import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import graphql.GraphQL;
import org.dataloader.MappedBatchLoader;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.datafetcher.DataLoaderRegistryHelper;
import software.wings.graphql.datafetcher.application.ApplicationDataFetcher;
import software.wings.graphql.datafetcher.application.ApplicationListDataFetcher;
import software.wings.graphql.datafetcher.application.batchloader.ApplicationBatchDataLoader;
import software.wings.graphql.datafetcher.artifact.ArtifactDataFetcher;
import software.wings.graphql.datafetcher.environment.EnvironmentDataFetcher;
import software.wings.graphql.datafetcher.environment.EnvironmentListDataFetcher;
import software.wings.graphql.datafetcher.workflow.WorkflowDataFetcher;
import software.wings.graphql.datafetcher.workflow.WorkflowExecutionDataFetcher;
import software.wings.graphql.datafetcher.workflow.WorkflowExecutionListDataFetcher;
import software.wings.graphql.datafetcher.workflow.WorkflowListDataFetcher;
import software.wings.graphql.directive.DataFetcherDirective;
import software.wings.graphql.provider.GraphQLProvider;
import software.wings.graphql.provider.QueryLanguageProvider;

import java.util.Collections;
import java.util.Set;

/**
 * Created a new module as part of code review comment
 */
public class GraphQLModule extends AbstractModule {
  public static final String WORKFLOW = "workflow";
  public static final String WORKFLOWS = "workflows";
  public static final String WORKFLOW_EXECUTION = "workflowExecution";
  public static final String WORKFLOW_EXECUTIONS = "workflowExecutions";
  public static final String DEPLOYED_ARTIFACTS = "deployedArtifacts";
  public static final String APPLICATION = "application";
  public static final String APPLICATIONS = "applications";
  public static final String ENVIRONMENT = "environment";
  public static final String ENVIRONMENTS = "environments";

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
    bindDataFetcherWithAnnotation(WORKFLOW, WorkflowDataFetcher.class);
    bindDataFetcherWithAnnotation(WORKFLOWS, WorkflowListDataFetcher.class);
    bindDataFetcherWithAnnotation(WORKFLOW_EXECUTION, WorkflowExecutionDataFetcher.class);
    bindDataFetcherWithAnnotation(WORKFLOW_EXECUTIONS, WorkflowExecutionListDataFetcher.class);
    bindDataFetcherWithAnnotation(DEPLOYED_ARTIFACTS, ArtifactDataFetcher.class);
    bindDataFetcherWithAnnotation(APPLICATION, ApplicationDataFetcher.class);
    bindDataFetcherWithAnnotation(ENVIRONMENT, EnvironmentDataFetcher.class);
    bindDataFetcherWithAnnotation(ENVIRONMENTS, EnvironmentListDataFetcher.class);
    bindDataFetcherWithAnnotation(APPLICATIONS, ApplicationListDataFetcher.class);
  }

  private void bindBatchedDataLoaders() {
    bindBatchedDataLoaderWithAnnotation(APPLICATION, ApplicationBatchDataLoader.class);
  }

  private void bindDataFetcherWithAnnotation(String annotationName, Class<? extends AbstractDataFetcher> childClass) {
    bind(AbstractDataFetcher.class).annotatedWith(Names.named(annotationName)).to(childClass);
  }

  private void bindBatchedDataLoaderWithAnnotation(
      String annotationName, Class<? extends MappedBatchLoader> childClass) {
    BATCH_DATA_LOADER_NAMES.add(annotationName);
    bind(MappedBatchLoader.class).annotatedWith(Names.named(annotationName)).to(childClass).in(Scopes.SINGLETON);
  }
}

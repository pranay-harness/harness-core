package software.wings.app;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.name.Names;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import graphql.GraphQL;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.datafetcher.DataFetcherEnum;
import software.wings.graphql.datafetcher.application.ApplicationDataFetcher;
import software.wings.graphql.datafetcher.application.ApplicationListDataFetcher;
import software.wings.graphql.datafetcher.artifact.ArtifactDataFetcher;
import software.wings.graphql.datafetcher.environment.EnvironmentDataFetcher;
import software.wings.graphql.datafetcher.environment.EnvironmentListDataFetcher;
import software.wings.graphql.datafetcher.workflow.WorkflowDataFetcher;
import software.wings.graphql.datafetcher.workflow.WorkflowExecutionDataFetcher;
import software.wings.graphql.datafetcher.workflow.WorkflowExecutionListDataFetcher;
import software.wings.graphql.datafetcher.workflow.WorkflowListDataFetcher;
import software.wings.graphql.provider.GraphQLProvider;
import software.wings.graphql.provider.QueryLanguageProvider;

/**
 * Created a new module as part of code review comment.q
 */
public class GraphQLModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(new TypeLiteral<QueryLanguageProvider<GraphQL>>() {}).to(GraphQLProvider.class);

    bindPostContructInitializationForGraphQL();

    bind(AbstractDataFetcher.class)
        .annotatedWith(Names.named(DataFetcherEnum.WORKFLOW.getDataFetcherName()))
        .to(WorkflowDataFetcher.class);

    bind(AbstractDataFetcher.class)
        .annotatedWith(Names.named(DataFetcherEnum.WORKFLOWS.getDataFetcherName()))
        .to(WorkflowListDataFetcher.class);

    bind(AbstractDataFetcher.class)
        .annotatedWith(Names.named(DataFetcherEnum.WORKFLOW_EXECUTION.getDataFetcherName()))
        .to(WorkflowExecutionDataFetcher.class);

    bind(AbstractDataFetcher.class)
        .annotatedWith(Names.named(DataFetcherEnum.WORKFLOW_EXECUTIONS.getDataFetcherName()))
        .to(WorkflowExecutionListDataFetcher.class);

    bind(AbstractDataFetcher.class)
        .annotatedWith(Names.named(DataFetcherEnum.DEPLOYED_ARTIFACTS.getDataFetcherName()))
        .to(ArtifactDataFetcher.class);

    bind(AbstractDataFetcher.class)
        .annotatedWith(Names.named(DataFetcherEnum.APPLICATION.getDataFetcherName()))
        .to(ApplicationDataFetcher.class);
    bind(AbstractDataFetcher.class)
        .annotatedWith(Names.named(DataFetcherEnum.ENVIRONMENT.getDataFetcherName()))
        .to(EnvironmentDataFetcher.class);

    bind(AbstractDataFetcher.class)
        .annotatedWith(Names.named(DataFetcherEnum.ENVIRONMENTS.getDataFetcherName()))
        .to(EnvironmentListDataFetcher.class);

    bind(AbstractDataFetcher.class)
        .annotatedWith(Names.named(DataFetcherEnum.APPLICATIONS.getDataFetcherName()))
        .to(ApplicationListDataFetcher.class);
  }

  /***
   * Custom logic for <code>@PostConstruct</code> initialization of a class.
   *
   * TODO
   * This method can be made generic
   */
  private void bindPostContructInitializationForGraphQL() {
    bindListener(
        new AbstractMatcher<TypeLiteral<?>>() {
          @Override
          public boolean matches(TypeLiteral<?> typeLiteral) {
            return typeLiteral.equals(TypeLiteral.get(GraphQLProvider.class));
          }
        },
        new TypeListener() {
          @Override
          public <I> void hear(final TypeLiteral<I> typeLiteral, TypeEncounter<I> typeEncounter) {
            typeEncounter.register(new InjectionListener<I>() {
              @Override
              public void afterInjection(Object i) {
                GraphQLProvider m = (GraphQLProvider) i;
                m.init();
              }
            });
          }
        });
  }
}

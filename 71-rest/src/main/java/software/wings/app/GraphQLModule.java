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
import software.wings.graphql.datafetcher.application.ApplicationDataFetcher;
import software.wings.graphql.datafetcher.application.ApplicationListDataFetcher;
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

/**
 * Created a new module as part of code review comment.q
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

  @Override
  protected void configure() {
    bind(new TypeLiteral<QueryLanguageProvider<GraphQL>>() {}).to(GraphQLProvider.class);

    bindPostContructInitializationForGraphQL();

    bind(DataFetcherDirective.class);

    bind(AbstractDataFetcher.class).annotatedWith(Names.named(WORKFLOW)).to(WorkflowDataFetcher.class);

    bind(AbstractDataFetcher.class).annotatedWith(Names.named(WORKFLOWS)).to(WorkflowListDataFetcher.class);

    bind(AbstractDataFetcher.class)
        .annotatedWith(Names.named(WORKFLOW_EXECUTION))
        .to(WorkflowExecutionDataFetcher.class);

    bind(AbstractDataFetcher.class)
        .annotatedWith(Names.named(WORKFLOW_EXECUTIONS))
        .to(WorkflowExecutionListDataFetcher.class);

    bind(AbstractDataFetcher.class).annotatedWith(Names.named(DEPLOYED_ARTIFACTS)).to(ArtifactDataFetcher.class);

    bind(AbstractDataFetcher.class).annotatedWith(Names.named(APPLICATION)).to(ApplicationDataFetcher.class);
    bind(AbstractDataFetcher.class).annotatedWith(Names.named(ENVIRONMENT)).to(EnvironmentDataFetcher.class);

    bind(AbstractDataFetcher.class).annotatedWith(Names.named(ENVIRONMENTS)).to(EnvironmentListDataFetcher.class);

    bind(AbstractDataFetcher.class).annotatedWith(Names.named(APPLICATIONS)).to(ApplicationListDataFetcher.class);
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

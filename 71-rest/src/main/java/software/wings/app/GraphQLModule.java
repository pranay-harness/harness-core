package software.wings.app;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import graphql.GraphQL;
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

package software.wings.graphql.provider;

public interface QueryLanguageProvider<T> {
  T getPrivateGraphQL();
  T getPublicGraphQL();
}

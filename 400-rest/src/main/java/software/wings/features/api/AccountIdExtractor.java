package software.wings.features.api;

public interface AccountIdExtractor<T> {
  String getAccountId(T obj);
}

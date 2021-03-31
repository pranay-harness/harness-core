package software.wings.delegatetasks.azure.common.validator;

@FunctionalInterface
public interface Validator<T> {
  void validate(T t);
}

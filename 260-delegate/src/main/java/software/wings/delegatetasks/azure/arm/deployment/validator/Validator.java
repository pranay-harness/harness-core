package software.wings.delegatetasks.azure.arm.deployment.validator;

@FunctionalInterface
public interface Validator<T> {
  void validate(T t);
}

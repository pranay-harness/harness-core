package software.wings.delegatetasks.azure.arm.deployment.validator;

public class Validators {
  private Validators() {}

  public static <T> void validate(T t, Validator<T> validator) {
    validator.validate(t);
  }
}

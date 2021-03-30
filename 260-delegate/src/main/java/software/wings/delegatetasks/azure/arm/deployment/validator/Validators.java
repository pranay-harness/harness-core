package software.wings.delegatetasks.azure.arm.deployment.validator;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.HibernateValidator;

public class Validators {
  private static final javax.validation.Validator validator = Validation.byProvider(HibernateValidator.class)
                                                                  .configure()
                                                                  .addProperty("hibernate.validator.fail_fast", "true")
                                                                  .buildValidatorFactory()
                                                                  .usingContext()
                                                                  .getValidator();
  private Validators() {}

  public static <T> void validate(T t, Validator<T> validator) {
    validator.validate(t);
  }

  public static <X extends Throwable> void validateJsr380FailFast(
      Object obj, Function<String, ? extends X> exceptionFunction) throws X {
    Set<ConstraintViolation<Object>> violations = validator.validate(obj);
    if (!violations.isEmpty()) {
      throw exceptionFunction.apply(toString(violations));
    }
  }

  private static String toString(Set<? extends ConstraintViolation<?>> constraintViolations) {
    return constraintViolations.stream()
        .map(cv -> cv == null ? StringUtils.EMPTY : cv.getMessage())
        .collect(Collectors.joining(", "));
  }
}

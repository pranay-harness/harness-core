package software.wings.helpers.ext.ldap;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;

import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;

public interface LdapValidator {
  default void validate(Logger logger) {
    Set<ConstraintViolation<Object>> violations =
        Validation.buildDefaultValidatorFactory().getValidator().validate(this);
    if (CollectionUtils.isNotEmpty(violations)) {
      for (ConstraintViolation violation : violations) {
        logger.error("{}.{} {}", violation.getRootBeanClass().getSimpleName(), violation.getPropertyPath(),
            violation.getMessage());
      }
      throw new ConstraintViolationException(violations);
    }
  }
}

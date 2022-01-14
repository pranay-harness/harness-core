package io.harness.resourcegroup.validator;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.resourcegroup.remote.dto.ManagedFilter;
import io.harness.resourcegroup.remote.dto.ResourceGroupFilterDTO;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@OwnedBy(HarnessTeam.PL)
public class ResourceGroupFilterValidator
    implements ConstraintValidator<ValidResourceGroupFilter, ResourceGroupFilterDTO> {
  @Override
  public void initialize(ValidResourceGroupFilter constraintAnnotation) {
    // nothing to initialize
  }

  @Override
  public boolean isValid(ResourceGroupFilterDTO value, ConstraintValidatorContext context) {
    if (isEmpty(value.getAccountIdentifier()) && !ManagedFilter.ONLY_MANAGED.equals(value.getManagedFilter())) {
      context.disableDefaultConstraintViolation();
      context
          .buildConstraintViolationWithTemplate(
              "Invalid Resource Group Filter: Either managed filter should be set to only managed, or scope filter should be non-empty")
          .addConstraintViolation();
      return false;
    }
    if (isNotEmpty(value.getAccountIdentifier()) && isNotEmpty(value.getScopeLevelFilter())) {
      context.disableDefaultConstraintViolation();
      context
          .buildConstraintViolationWithTemplate(
              "Invalid Resource Group Filter: Either scope filter or scope level filter but not both should be provided")
          .addConstraintViolation();
      return false;
    }
    return true;
  }
}

package software.wings.delegatetasks.azure.common.validator;

import software.wings.utils.ArtifactType;

import java.util.Arrays;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ArtifactTypeSubsetValidator implements ConstraintValidator<ArtifactTypeSubset, ArtifactType> {
  ArtifactType[] subset;

  @Override
  public void initialize(ArtifactTypeSubset artifactTypeSubset) {
    this.subset = artifactTypeSubset.anyOf();
  }

  @Override
  public boolean isValid(ArtifactType value, ConstraintValidatorContext constraintValidatorContext) {
    return value == null || Arrays.asList(subset).contains(value);
  }
}

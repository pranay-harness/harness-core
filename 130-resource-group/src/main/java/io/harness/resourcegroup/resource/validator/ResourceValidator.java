package io.harness.resourcegroup.resource.validator;

import java.util.List;

public interface ResourceValidator {
  boolean validate(List<String> resourceIds, String accountIdentifier, String orgIdentifier, String projectIdentifier);
}

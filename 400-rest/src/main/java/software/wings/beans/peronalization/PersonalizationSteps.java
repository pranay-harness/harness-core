/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.peronalization;

import java.util.LinkedList;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "PersonalizationStepsKeys")
public class PersonalizationSteps {
  private Set<String> favorites;
  private LinkedList<String> recent;
}

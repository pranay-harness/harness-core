/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.features.api;

import java.util.Map;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.ToString;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@ToString
public class Usage {
  @NonNull @NotEmpty String entityId;
  @NonNull @NotEmpty String entityType;
  @NonNull @NotEmpty String entityName;
  @Singular Map<String, String> properties;
}

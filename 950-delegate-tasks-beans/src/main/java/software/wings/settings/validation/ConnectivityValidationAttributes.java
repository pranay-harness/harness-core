/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.settings.validation;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXISTING_PROPERTY;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.hibernate.validator.constraints.NotEmpty;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = EXISTING_PROPERTY)
public abstract class ConnectivityValidationAttributes {
  @NotEmpty private String type;
}

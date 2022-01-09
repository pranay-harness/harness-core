/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.scim;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScimMultiValuedObject<T> {
  String type;
  boolean primary;
  String display;
  T value;
  @JsonProperty("$ref") URI ref;

  String id;
  String displayName;
  boolean active;
}

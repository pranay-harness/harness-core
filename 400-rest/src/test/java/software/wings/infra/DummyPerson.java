/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.infra;

import software.wings.annotation.CustomFieldMapKey;
import software.wings.annotation.IncludeFieldMap;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DummyPerson implements FieldKeyValMapProvider {
  @IncludeFieldMap @CustomFieldMapKey("customKey") private String id;
  @IncludeFieldMap private String name;
  private String occupation;
}

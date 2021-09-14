/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

/**
 * The mini view of entities included
 * in a search preview.
 *
 * @author utkarsh
 */
@OwnedBy(PL)
@Value
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "EntityInfoKeys")
public class EntityInfo {
  String id;
  String name;
}

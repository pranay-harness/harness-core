/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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

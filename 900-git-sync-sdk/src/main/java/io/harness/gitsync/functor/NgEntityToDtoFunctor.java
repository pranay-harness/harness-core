/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.functor;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.beans.NGPersistentEntity;
import io.harness.gitsync.beans.YamlDTO;

@FunctionalInterface
@OwnedBy(DX)
public interface NgEntityToDtoFunctor<E extends NGPersistentEntity, Y extends YamlDTO> {
  Y apply(E e);
}

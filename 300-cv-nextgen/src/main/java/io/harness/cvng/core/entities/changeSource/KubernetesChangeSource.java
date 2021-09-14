/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.entities.changeSource;

import io.harness.mongo.index.FdIndex;

import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.query.UpdateOperations;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class KubernetesChangeSource extends ChangeSource {
  @NotNull @FdIndex String connectorIdentifier;

  public static class UpdatableKubernetesChangeSourceEntity
      extends UpdatableChangeSourceEntity<KubernetesChangeSource, KubernetesChangeSource> {
    @Override
    public void setUpdateOperations(UpdateOperations<KubernetesChangeSource> updateOperations,
        KubernetesChangeSource harnessKubernetesChangeSource) {
      /* NO-OP should not update fields here */
    }
  }
}

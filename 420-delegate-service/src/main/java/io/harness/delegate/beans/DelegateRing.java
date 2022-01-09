/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.PersistentEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@FieldNameConstants(innerTypeName = "DelegateRingKeys")
@Entity(value = "delegateRing", noClassnameStored = true)
@HarnessEntity(exportable = false)
@OwnedBy(HarnessTeam.DEL)
public class DelegateRing implements PersistentEntity {
  public DelegateRing(String ringName, String delegateImageTag, String upgraderImageTag) {
    this.ringName = ringName;
    this.delegateImageTag = delegateImageTag;
    this.upgraderImageTag = upgraderImageTag;
  }

  @Id @NotEmpty private String ringName;
  @NotEmpty private String delegateImageTag;
  @NotEmpty private String upgraderImageTag;
}

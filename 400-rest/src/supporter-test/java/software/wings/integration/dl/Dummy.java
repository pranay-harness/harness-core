/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.integration.dl;

import io.harness.annotation.HarnessEntity;

import software.wings.beans.Base;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity(value = "!!!testDummies", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class Dummy extends Base {
  private List<DummyItem> dummies;
  private String name;
}

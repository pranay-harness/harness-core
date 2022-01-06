/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.mongo.changestreams;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static lombok.AccessLevel.PRIVATE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.PersistentEntity;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.bson.conversions.Bson;

/**
 * The change tracking info that has to
 * be provided to open a changestream.
 *
 * @author utkarsh
 */

@OwnedBy(PL)
@Value
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class ChangeTrackingInfo<T extends PersistentEntity> {
  Class<T> morphiaClass;
  ChangeSubscriber<T> changeSubscriber;
  String resumeToken;
  List<Bson> pipeline;
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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

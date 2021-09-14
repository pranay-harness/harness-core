/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ci.beans.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.persistence.PersistentEntity;
import io.harness.validation.Update;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "BuildNumberDetailsKeys")
@Entity(value = "buildNumberDetails", noClassnameStored = true)
@StoreIn("harnessci")
@Document("buildNumberDetails")
@HarnessEntity(exportable = true)
@TypeAlias("buildNumberDetails")
public class BuildNumberDetails implements PersistentEntity {
  @Builder.Default private Long buildNumber = 0L;
  @Id @org.springframework.data.annotation.Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
}

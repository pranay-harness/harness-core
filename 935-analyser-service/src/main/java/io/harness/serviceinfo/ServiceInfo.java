/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serviceinfo;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.FdUniqueIndex;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.Wither;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(PIPELINE)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "ServiceInfoKeys")
@Entity(value = "serviceInfos", noClassnameStored = true)
@Document("serviceInfos")
@TypeAlias("serviceInfo")
public class ServiceInfo {
  @Id @org.mongodb.morphia.annotations.Id String uuid;
  @FdUniqueIndex @NonNull String serviceId;
  // Cannot be set rollbacks? Need to be a list as we need order
  @Singular List<String> versions;

  @Wither @CreatedDate Long createdAt;
  @Wither @LastModifiedDate Long updatedAt;
}

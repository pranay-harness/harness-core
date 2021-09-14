/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.mongo.index.FdIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@FieldNameConstants(innerTypeName = "PrimaryVersionKeys")
@Entity(value = "primaryVersion", noClassnameStored = true)
@HarnessEntity(exportable = false)
@StoreIn(DbAliases.ALL)
public final class PrimaryVersion implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  public static final String GLOBAL_CONFIG_ID = "__GLOBAL_CONFIG_ID__";
  public static final String MATCH_ALL_VERSION = "*";
  @Id private String uuid;
  @FdIndex private long createdAt;
  @FdIndex private long lastUpdatedAt;
  private String primaryVersion;

  public static final class Builder {
    String primaryVersion;

    private Builder() {}

    public static PrimaryVersion.Builder aConfiguration() {
      return new PrimaryVersion.Builder();
    }

    public Builder withPrimaryVersion(String primaryVersion) {
      this.primaryVersion = primaryVersion;
      return this;
    }

    public PrimaryVersion build() {
      PrimaryVersion primaryVersion = new PrimaryVersion();
      primaryVersion.setUuid(GLOBAL_CONFIG_ID);
      primaryVersion.setPrimaryVersion(this.primaryVersion);
      return primaryVersion;
    }
  }
}

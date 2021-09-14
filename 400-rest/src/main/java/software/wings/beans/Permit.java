/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.FdUniqueIndex;

import java.util.Date;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;

/**
 * Created by anubhaw on 7/17/18.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity(value = "permits", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class Permit extends Base {
  public static final String PERMIT_KEY_ID = "key";
  @FdUniqueIndex private String key;
  private String group;
  @FdTtlIndex private Date expireAt;
  private long leaseDuration;
  @FdIndex private String accountId;

  @Builder
  public Permit(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, String entityYamlPath, String key, String group, Date expireAt, long leaseDuration,
      String accountId) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.key = key;
    this.group = group;
    this.expireAt = new Date(expireAt.getTime());
    this.leaseDuration = leaseDuration;
    this.accountId = accountId;
  }
}

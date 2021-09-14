/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.security;

import static io.harness.annotations.dev.HarnessModule._970_RBAC_CORE;
import static io.harness.annotations.dev.HarnessTeam.PL;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EmbeddedUser;
import io.harness.mongo.index.FdIndex;

import software.wings.beans.Base;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;

/**
 * User bean class.
 *
 * @author Rishi
 */

@JsonInclude(NON_EMPTY)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity(value = "harnessUserGroups", noClassnameStored = true)
@HarnessEntity(exportable = true)
@OwnedBy(PL)
@TargetModule(_970_RBAC_CORE)
public class HarnessUserGroup extends Base {
  @NotEmpty private String name;
  private String description;
  @FdIndex private Set<String> memberIds;
  private Set<String> accountIds;
  private GroupType groupType;
  public enum GroupType { DEFAULT, RESTRICTED }

  @Builder
  public HarnessUserGroup(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, String entityYamlPath, String name, String description, Set<String> memberIds,
      Set<String> accountIds, GroupType groupType) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.name = name;
    this.description = description;
    this.memberIds = memberIds;
    this.accountIds = accountIds;
    this.groupType = groupType;
  }
}

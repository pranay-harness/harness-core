/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.common.EntityReference;
import io.harness.common.EntityReferenceHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.encryption.Scope;

import java.util.LinkedList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDC)
public class NGTemplateReference implements EntityReference {
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String identifier;

  String versionLabel;
  String repoIdentifier;
  String branch;
  Boolean isDefault;
  Scope scope;

  @Override
  public String getFullyQualifiedName() {
    List<String> fqnList = new LinkedList<>();
    fqnList.add(accountIdentifier);
    if (EmptyPredicate.isNotEmpty(orgIdentifier)) {
      fqnList.add(orgIdentifier);
    }
    if (EmptyPredicate.isNotEmpty(projectIdentifier)) {
      fqnList.add(projectIdentifier);
    }
    fqnList.add(identifier);
    fqnList.add(versionLabel);

    return EntityReferenceHelper.createFQN(fqnList);
  }

  @Override
  public Boolean isDefault() {
    return isDefault;
  }

  @Override
  public void setBranch(String branch) {
    this.branch = branch;
  }

  @Override
  public void setRepoIdentifier(String repoIdentifier) {
    this.repoIdentifier = repoIdentifier;
  }

  @Override
  public void setIsDefault(Boolean isDefault) {
    this.isDefault = isDefault;
  }
}

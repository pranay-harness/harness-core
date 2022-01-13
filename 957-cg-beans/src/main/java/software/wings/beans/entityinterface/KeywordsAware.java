/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.entityinterface;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.persistence.CreatedByAccess;
import io.harness.persistence.UpdatedByAccess;

import java.util.HashSet;
import java.util.Set;

@OwnedBy(HarnessTeam.CDC)
public interface KeywordsAware {
  Set<String> getKeywords();

  void setKeywords(Set<String> keywords);

  default Set<String> generateKeywords() {
    Set<String> keyWordList = new HashSet<>();

    if (this instanceof CreatedByAccess) {
      CreatedByAccess createdByAccess = (CreatedByAccess) this;
      final EmbeddedUser createdBy = createdByAccess.getCreatedBy();
      if (createdBy != null) {
        keyWordList.add(createdBy.getName());
        keyWordList.add(createdBy.getEmail());
      }
    }

    if (this instanceof UpdatedByAccess) {
      UpdatedByAccess updatedByAccess = (UpdatedByAccess) this;
      final EmbeddedUser updatedBy = updatedByAccess.getLastUpdatedBy();
      if (updatedBy != null) {
        keyWordList.add(updatedBy.getName());
        keyWordList.add(updatedBy.getEmail());
      }
    }

    return keyWordList;
  }
}

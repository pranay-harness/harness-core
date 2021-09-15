/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.entities;

import io.harness.ChangeHandler;
import io.harness.changehandlers.ProjectsChangeDataHandler;
import io.harness.changehandlers.TagsInfoCDChangeDataHandler;
import io.harness.ng.core.entities.Project;
import io.harness.persistence.PersistentEntity;

import com.google.inject.Inject;

public class ProjectEntity implements CDCEntity<Project> {
  @Inject private ProjectsChangeDataHandler projectsChangeDataHandler;
  @Inject private TagsInfoCDChangeDataHandler tagsInfoCDChangeDataHandler;

  @Override
  public ChangeHandler getChangeHandler(String handlerClass) {
    if (handlerClass.contentEquals("TagsInfoCD")) {
      return tagsInfoCDChangeDataHandler;
    }
    return projectsChangeDataHandler;
  }

  @Override
  public Class<? extends PersistentEntity> getSubscriptionEntity() {
    return Project.class;
  }
}

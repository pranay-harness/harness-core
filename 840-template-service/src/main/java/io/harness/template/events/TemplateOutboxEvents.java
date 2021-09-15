/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.template.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CDC)
public class TemplateOutboxEvents {
  public static final String TEMPLATE_VERSION_CREATED = "TemplateVersionCreated";
  public static final String TEMPLATE_VERSION_UPDATED = "TemplateVersionUpdated";
  public static final String TEMPLATE_VERSION_DELETED = "TemplateVersionDeleted";
}

/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.intfc.yaml.clone;

import io.harness.rest.RestResponse;

public interface YamlCloneService {
  RestResponse cloneEntityUsingYaml(
      String accountId, String appId, boolean includeFiles, String entityType, String entiytId, String newName);
}

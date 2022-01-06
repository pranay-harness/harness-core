/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.visitor.helpers.manifest;

import io.harness.cdng.manifest.yaml.kinds.KustomizeManifest;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class KustomizeManifestVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {}

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    KustomizeManifest kustomizeManifest = (KustomizeManifest) originalElement;
    return KustomizeManifest.builder().identifier(kustomizeManifest.getIdentifier()).build();
  }
}

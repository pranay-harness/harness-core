/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.serializer.morphia;

import io.harness.beans.WithIdentifier;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.NGAccountAccess;
import io.harness.ng.core.NGOrgAccess;
import io.harness.ng.core.NGProjectAccess;

import java.util.Set;

public class NGCommonsMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(NGAccountAccess.class);
    set.add(NGAccess.class);
    set.add(NGProjectAccess.class);
    set.add(NGOrgAccess.class);
    set.add(WithIdentifier.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {}
}

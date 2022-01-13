/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public abstract class AbstractObjectDataFetcher<T, P> extends PlainObjectBaseDataFetcher<T, P> {
  protected abstract T fetch(P parameters, String accountId);

  @Override
  protected Object fetchPlainObject(P parameters, String accountId) {
    return fetch(parameters, accountId);
  }
}

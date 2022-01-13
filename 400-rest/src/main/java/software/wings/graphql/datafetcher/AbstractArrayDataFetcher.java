/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public abstract class AbstractArrayDataFetcher<T, P> extends PlainObjectBaseDataFetcher<T, P> {
  protected abstract List<T> fetch(P parameters, String accountId);

  // In get() of PlainObjectBaseDataFetcher, we are trying to find the return class
  // Class<T> returnClass = (Class<T>) typeArguments[0];
  // However Java is optimizing and not passing the generic <T> assuming that this is not used.
  // To get around this, we are creating a dummy method so that Java think that this will be used downstream and
  // doesn't do any optimization around that.
  protected abstract T unusedReturnTypePassingDummyMethod();

  @Override
  protected Object fetchPlainObject(P parameters, String accountId) {
    return fetch(parameters, accountId);
  }
}

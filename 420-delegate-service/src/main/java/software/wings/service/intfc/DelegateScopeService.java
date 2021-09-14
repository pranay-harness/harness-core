/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.beans.DelegateScope;

import javax.validation.Valid;

@OwnedBy(DEL)
public interface DelegateScopeService {
  /**
   * List page response.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<DelegateScope> list(PageRequest<DelegateScope> pageRequest);

  /**
   * Get delegate.
   *
   * @param accountId  the account id
   * @param delegateScopeId the delegate scope id
   * @return the delegate
   */
  DelegateScope get(String accountId, String delegateScopeId);

  /**
   * Get delegate.
   *
   * @param accountId  the account id
   * @param name the delegate scope name
   * @return the delegate
   */
  DelegateScope getByName(String accountId, String name);

  /**
   * Update delegate.
   *
   * @param delegateScope the delegate scope
   * @return the delegate
   */
  DelegateScope update(@Valid DelegateScope delegateScope);

  /**
   * Add delegate scope.
   *
   * @param delegateScope the delegate scope
   * @return the delegate scope
   */
  DelegateScope add(DelegateScope delegateScope);

  /**
   * Delete.
   *
   * @param accountId  the account id
   * @param delegateScopeId the delegate scope id
   */
  void delete(String accountId, String delegateScopeId);
}

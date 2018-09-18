package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import software.wings.beans.DelegateScope;

import javax.validation.Valid;

/**
 * Created by brett on 8/4/17
 */
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

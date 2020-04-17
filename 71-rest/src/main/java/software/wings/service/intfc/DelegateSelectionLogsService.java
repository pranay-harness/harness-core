package software.wings.service.intfc;

import software.wings.beans.DelegateScope;

public interface DelegateSelectionLogsService {
  void logIncludeScopeMatched(DelegateScope scope, String delegateId);

  void logExcludeScopeMatched(DelegateScope scope, String delegateId);

  void logMissingSelector(String selector, String delegateId);

  void logMissingAllSelectors(String delegateId);

  void logNoIncludeScopeMatched(String delegateId);
}

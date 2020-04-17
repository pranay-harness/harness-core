package software.wings.service.impl;

import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.DelegateScope;
import software.wings.service.intfc.DelegateSelectionLogsService;

@Singleton
@Slf4j
public class DelegateSelectionLogsServiceImpl implements DelegateSelectionLogsService {
  private static final String REJECTED = "Rejected";
  private static final String ACCEPTED = "Accepted";

  @Override
  public void logIncludeScopeMatched(DelegateScope scope, String delegateId) {
    logger.info("{} - Matched include scope '{}', for delegate '{}'", ACCEPTED, scope, delegateId);
  }

  @Override
  public void logNoIncludeScopeMatched(String delegateId) {
    logger.info("{} - No matching include scope for delegate {}", REJECTED, delegateId);
  }

  @Override
  public void logExcludeScopeMatched(DelegateScope scope, String delegateId) {
    logger.info("{} - Matched exclude scope '{}', for delegate '{}'", REJECTED, scope, delegateId);
  }

  @Override
  public void logMissingSelector(String selector, String delegateId) {
    logger.info("{} - Missing selector '{}' for delegateId '{}'", REJECTED, selector, delegateId);
  }

  @Override
  public void logMissingAllSelectors(String delegateId) {
    logger.info("{} - Missing all selectors for delegate {}", REJECTED, delegateId);
  }
}

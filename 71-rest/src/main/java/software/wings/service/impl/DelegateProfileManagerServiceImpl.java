package software.wings.service.impl;

import com.google.inject.Singleton;

import io.harness.delegate.beans.DelegateProfileDetails;
import io.harness.delegate.beans.ScopingRuleDetails;
import io.harness.exception.UnsupportedOperationException;
import lombok.extern.slf4j.Slf4j;
import software.wings.service.intfc.DelegateProfileManagerService;

import java.util.List;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
@Slf4j
public class DelegateProfileManagerServiceImpl implements DelegateProfileManagerService {
  @Override
  public List<DelegateProfileDetails> list(String accountId) {
    logger.info("List delegate profiles");
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public DelegateProfileDetails get(String accountId, String delegateProfileId) {
    logger.info("Get delegate profile");
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public DelegateProfileDetails update(DelegateProfileDetails delegateProfile) {
    logger.info("Update delegate profile");
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public DelegateProfileDetails updateScopingRules(
      String accountId, String delegateProfileId, List<ScopingRuleDetails> scopingRules) {
    logger.info("Update delegate profile scoping rules");
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public DelegateProfileDetails add(DelegateProfileDetails delegateProfile) {
    logger.info("Add delegate profile");
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void delete(String accountId, String delegateProfileId) {
    logger.info("Delete delegate profile");
    throw new UnsupportedOperationException("not implemented");
  }
}
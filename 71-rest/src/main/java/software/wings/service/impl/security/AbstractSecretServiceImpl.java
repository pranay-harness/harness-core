package software.wings.service.impl.security;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;

/**
 * Created by rsingh on 11/6/17.
 */
public abstract class AbstractSecretServiceImpl {
  protected static final Logger logger = LoggerFactory.getLogger(AbstractSecretServiceImpl.class);
  @Inject protected WingsPersistence wingsPersistence;
  @Inject protected DelegateProxyFactory delegateProxyFactory;
}

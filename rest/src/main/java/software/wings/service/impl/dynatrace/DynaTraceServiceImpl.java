package software.wings.service.impl.dynatrace;

import com.google.inject.Inject;

import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.dynatrace.DynaTraceService;
import software.wings.service.intfc.security.SecretManager;

/**
 * Created by rsingh on 1/30/18.
 */
public class DynaTraceServiceImpl implements DynaTraceService {
  @Inject private SettingsService settingsService;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private SecretManager secretManager;
}

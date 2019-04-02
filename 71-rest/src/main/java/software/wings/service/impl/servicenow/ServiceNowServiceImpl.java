package software.wings.service.impl.servicenow;

import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.DelegateTask.DEFAULT_SYNC_CALL_TIMEOUT;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ServiceNowConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SyncTaskContext;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.servicenow.ServiceNowDelegateService;
import software.wings.service.intfc.servicenow.ServiceNowService;

@Singleton
public class ServiceNowServiceImpl implements ServiceNowService {
  private static final Logger logger = LoggerFactory.getLogger(ServiceNowServiceImpl.class);
  private static final String APP_ID_KEY = "appId";
  private static final long DELEGATE_TIMEOUT_MILLIS = 60 * 1000;

  @Inject private DelegateService delegateService;
  @Inject protected DelegateProxyFactory delegateProxyFactory;

  @Override
  public void validateCredential(SettingAttribute settingAttribute) {
    SyncTaskContext snowTaskContext = SyncTaskContext.builder()
                                          .accountId(settingAttribute.getAccountId())
                                          .appId(GLOBAL_APP_ID)
                                          .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                          .build();

    delegateProxyFactory.get(ServiceNowDelegateService.class, snowTaskContext)
        .validateConnector((ServiceNowConfig) settingAttribute.getValue());
  }
}

package software.wings.service.impl.elk;

import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.ElkConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SplunkConfig;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.exception.WingsException;
import software.wings.service.intfc.elk.ElkDelegateService;
import software.wings.service.intfc.elk.ElkService;
import software.wings.service.intfc.splunk.SplunkDelegateService;

import javax.inject.Inject;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by rsingh on 08/01/17.
 */
@ValidateOnExecution
public class ElkServiceImpl implements ElkService {
  private static final Logger logger = LoggerFactory.getLogger(ElkServiceImpl.class);

  @Inject private DelegateProxyFactory delegateProxyFactory;

  @Override
  public void validateConfig(final SettingAttribute settingAttribute) {
    try {
      SyncTaskContext syncTaskContext =
          aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
      delegateProxyFactory.get(ElkDelegateService.class, syncTaskContext)
          .validateConfig((ElkConfig) settingAttribute.getValue());
    } catch (Exception e) {
      throw new WingsException(ErrorCode.ELK_CONFIGURATION_ERROR, "reason", e.getMessage());
    }
  }
}

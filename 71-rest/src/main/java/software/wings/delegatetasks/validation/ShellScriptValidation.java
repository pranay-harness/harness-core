package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;
import static software.wings.common.Constants.HARNESS_KUBE_CONFIG_PATH;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AzureConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.delegation.ShellScriptParameters;
import software.wings.delegatetasks.validation.DelegateConnectionResult.DelegateConnectionResultBuilder;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.settings.SettingValue;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class ShellScriptValidation extends AbstractDelegateValidateTask {
  @Inject private transient ContainerValidationHelper containerValidationHelper;
  @Inject private transient ShellScriptValidationHandler shellScriptValidationHandler;

  public ShellScriptValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    Object[] parameters = getParameters();
    return singletonList(validate((ShellScriptParameters) parameters[0]));
  }

  private DelegateConnectionResult validate(ShellScriptParameters parameters) {
    DelegateConnectionResultBuilder resultBuilder = DelegateConnectionResult.builder().criteria(getCriteria().get(0));
    resultBuilder.validated(shellScriptValidationHandler.handle(parameters));
    return resultBuilder.build();
  }

  @Override
  public List<String> getCriteria() {
    ShellScriptParameters parameters = (ShellScriptParameters) getParameters()[0];

    String criteria;
    if (parameters.isExecuteOnDelegate()) {
      criteria = "localhost";
      ContainerServiceParams containerServiceParams = parameters.getContainerServiceParams();
      if (containerServiceParams != null) {
        SettingAttribute settingAttribute = containerServiceParams.getSettingAttribute();
        if (settingAttribute != null) {
          SettingValue value = settingAttribute.getValue();
          boolean useKubernetesDelegate =
              value instanceof KubernetesClusterConfig && ((KubernetesClusterConfig) value).isUseKubernetesDelegate();
          boolean isKubernetes = value instanceof KubernetesConfig || value instanceof GcpConfig
              || value instanceof AzureConfig || value instanceof KubernetesClusterConfig;
          if (useKubernetesDelegate || (isKubernetes && parameters.getScript().contains(HARNESS_KUBE_CONFIG_PATH))) {
            criteria = containerValidationHelper.getCriteria(containerServiceParams);
          }
        }
      }
    } else {
      criteria = parameters.getHost();
    }
    return singletonList(criteria);
  }
}

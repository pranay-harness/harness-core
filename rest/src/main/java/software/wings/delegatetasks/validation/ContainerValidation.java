package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static software.wings.utils.HttpUtil.connectableHttpUrl;

import org.apache.commons.lang3.StringUtils;
import software.wings.beans.AwsConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.delegatetasks.validation.DelegateConnectionResult.DelegateConnectionResultBuilder;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.settings.SettingValue;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/29/17
 */
public class ContainerValidation extends AbstractDelegateValidateTask {
  public ContainerValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    String criteria = getCriteria().get(0);

    DelegateConnectionResultBuilder result = DelegateConnectionResult.builder().criteria(criteria);

    if ("none".equals(criteria)) {
      result.validated(false);
    } else if ("GCP".equals(criteria)) {
      result.validated(true);
    } else if (StringUtils.startsWith(criteria, "AWS")) {
      result.validated(AwsHelperService.isInAwsRegion(substringAfter(criteria, ":")));
    } else {
      result.validated(connectableHttpUrl(criteria));
    }

    return singletonList(result.build());
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(getCriteria((ContainerServiceParams) getParameters()[2]));
  }

  private String getCriteria(ContainerServiceParams containerServiceParams) {
    SettingValue value = containerServiceParams.getSettingAttribute().getValue();

    if (value instanceof KubernetesConfig) {
      return ((KubernetesConfig) value).getMasterUrl();
    }

    if (value instanceof GcpConfig) {
      return "GCP";
    }

    if (value instanceof AwsConfig) {
      return "AWS:" + containerServiceParams.getRegion();
    }

    return "none";
  }
}

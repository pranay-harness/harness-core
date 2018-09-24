package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import software.wings.beans.DelegateTask;
import software.wings.beans.PrometheusConfig;
import software.wings.service.impl.prometheus.PrometheusDataCollectionInfo;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public class PrometheusValidation extends AbstractDelegateValidateTask {
  public PrometheusValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(
        Arrays.stream(getParameters())
            .filter(o -> o instanceof PrometheusDataCollectionInfo || o instanceof PrometheusConfig)
            .map(obj
                -> (obj instanceof PrometheusConfig ? (PrometheusConfig) obj
                                                    : ((PrometheusDataCollectionInfo) obj).getPrometheusConfig())
                       .getUrl())
            .findFirst()
            .orElse(null));
  }
}

package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import org.mongodb.morphia.annotations.Transient;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.request.ManifestAwareTaskParams;
import software.wings.helpers.ext.kustomize.KustomizeConfig;

import java.util.List;
import java.util.function.Consumer;

public class K8sCommandValidation extends AbstractDelegateValidateTask {
  @Inject @Transient private transient K8sValidationHelper k8sValidationHelper;

  public K8sCommandValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    K8sTaskParameters taskParameters = (K8sTaskParameters) getParameters()[0];
    K8sClusterConfig k8sClusterConfig = taskParameters.getK8sClusterConfig();
    final String criteria = getCriteria().get(0);
    boolean validated = k8sValidationHelper.validateContainerServiceParams(k8sClusterConfig);
    if (validated && k8sValidationHelper.kustomizeValidationNeeded(taskParameters)) {
      validated = k8sValidationHelper.doesKustomizePluginDirExist(
          ((ManifestAwareTaskParams) taskParameters).getK8sDelegateManifestConfig().getKustomizeConfig());
    }
    return singletonList(DelegateConnectionResult.builder().criteria(criteria).validated(validated).build());
  }

  private String getKustomizeCriteria(KustomizeConfig kustomizeConfig) {
    return k8sValidationHelper.getKustomizeCriteria(kustomizeConfig);
  }

  @Override
  public List<String> getCriteria() {
    K8sTaskParameters request = (K8sTaskParameters) getParameters()[0];
    StringBuilder criteria = new StringBuilder(getK8sCriteria(request.getK8sClusterConfig()));
    if (k8sValidationHelper.kustomizeValidationNeeded(request)) {
      String kustomizeCriteria =
          getKustomizeCriteria(((ManifestAwareTaskParams) request).getK8sDelegateManifestConfig().getKustomizeConfig());
      if (isNotBlank(kustomizeCriteria)) {
        criteria.append("||");
        criteria.append(kustomizeCriteria);
      }
    }
    return singletonList(criteria.toString());
  }

  private String getK8sCriteria(K8sClusterConfig k8sClusterConfig) {
    return k8sValidationHelper.getCriteria(k8sClusterConfig);
  }
}

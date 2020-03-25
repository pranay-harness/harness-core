package software.wings.graphql.datafetcher.cloudefficiencyevents;

import com.google.inject.Inject;

import io.harness.ccm.cluster.K8sYamlServiceImpl;
import io.harness.ccm.cluster.entities.K8sYaml;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.datafetcher.cloudefficiencyevents.QLK8sEventYamls.QLK8sEventYamlsBuilder;
import software.wings.graphql.schema.query.QLK8sEventYamlDiffQueryParameters;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

@Slf4j
public class K8sEventYamlDiffDataFetcher
    extends AbstractObjectDataFetcher<QLK8sEventYamlDiff, QLK8sEventYamlDiffQueryParameters> {
  @Inject K8sYamlServiceImpl k8sYamlService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLK8sEventYamlDiff fetch(QLK8sEventYamlDiffQueryParameters qlQuery, String accountId) {
    QLK8sEventYamlDiff yamlDiff = null;
    if (qlQuery != null) {
      K8sYaml oldYaml = k8sYamlService.get(accountId, qlQuery.getOldYamlRef());
      K8sYaml newYaml = k8sYamlService.get(accountId, qlQuery.getNewYamlRef());
      QLK8sEventYamlsBuilder builder = QLK8sEventYamls.builder();
      if (oldYaml != null) {
        builder.oldYaml(oldYaml.getYaml());
      }
      if (newYaml != null) {
        builder.newYaml(newYaml.getYaml());
      }
      yamlDiff = QLK8sEventYamlDiff.builder().data(builder.build()).build();
    }
    return yamlDiff;
  }
}

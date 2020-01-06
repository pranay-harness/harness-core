package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ec2.model.Filter;
import org.jetbrains.annotations.NotNull;
import software.wings.api.DeploymentType;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsInstanceFilter;
import software.wings.common.InfrastructureConstants;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.infra.AwsInstanceInfrastructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Singleton
public class AwsUtils {
  @Inject private ManagerExpressionEvaluator expressionEvaluator;

  public String getHostnameFromPrivateDnsName(String dnsName) {
    return isNotEmpty(dnsName) ? dnsName.split("\\.")[0] : "";
  }

  public String getHostnameFromConvention(Map<String, Object> context, String hostNameConvention) {
    if (isEmpty(hostNameConvention)) {
      hostNameConvention = InfrastructureConstants.DEFAULT_AWS_HOST_NAME_CONVENTION;
    }
    return expressionEvaluator.substitute(hostNameConvention, context);
  }

  public List<Filter> getAwsFilters(AwsInfrastructureMapping awsInfrastructureMapping, DeploymentType deploymentType) {
    AwsInstanceFilter instanceFilter = awsInfrastructureMapping.getAwsInstanceFilter();
    return getFilters(deploymentType, instanceFilter);
  }

  public List<Filter> getAwsFilters(
      AwsInstanceInfrastructure awsInstanceInfrastructure, DeploymentType deploymentType) {
    AwsInstanceFilter instanceFilter = awsInstanceInfrastructure.getAwsInstanceFilter();
    return getFilters(deploymentType, instanceFilter);
  }

  @NotNull
  public List<Filter> getFilters(DeploymentType deploymentType, AwsInstanceFilter instanceFilter) {
    List<Filter> filters = new ArrayList<>();
    filters.add(new Filter("instance-state-name").withValues("running"));
    if (instanceFilter != null) {
      if (isNotEmpty(instanceFilter.getVpcIds())) {
        filters.add(new Filter("vpc-id", instanceFilter.getVpcIds()));
      }
      if (isNotEmpty(instanceFilter.getTags())) {
        Multimap<String, String> tags = ArrayListMultimap.create();
        instanceFilter.getTags().forEach(tag -> tags.put(tag.getKey(), tag.getValue()));
        tags.keySet().forEach(key -> filters.add(new Filter("tag:" + key, new ArrayList<>(tags.get(key)))));
      }
      if (DeploymentType.WINRM == deploymentType) {
        filters.add(new Filter("platform", asList("windows")));
      }
    }
    return filters;
  }
}

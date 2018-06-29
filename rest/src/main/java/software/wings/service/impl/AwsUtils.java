package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ec2.model.Filter;
import org.apache.commons.lang3.StringUtils;
import software.wings.api.DeploymentType;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsInstanceFilter;
import software.wings.common.Constants;
import software.wings.expression.ExpressionEvaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Singleton
public class AwsUtils {
  @Inject private ExpressionEvaluator expressionEvaluator;

  public String getHostnameFromPrivateDnsName(String dnsName) {
    return isNotEmpty(dnsName) ? dnsName.split("\\.")[0] : "";
  }

  public String getHostnameFromConvention(Map<String, Object> context, String hostNameConvention) {
    if (isEmpty(hostNameConvention)) {
      hostNameConvention = Constants.DEFAULT_AWS_HOST_NAME_CONVENTION;
    }
    return expressionEvaluator.substitute(hostNameConvention, context);
  }

  public List<Filter> getAwsFilters(AwsInfrastructureMapping awsInfrastructureMapping) {
    AwsInstanceFilter instanceFilter = awsInfrastructureMapping.getAwsInstanceFilter();
    List<Filter> filters = new ArrayList<>();
    filters.add(new Filter("instance-state-name").withValues("running"));
    if (instanceFilter != null) {
      if (isNotEmpty(instanceFilter.getVpcIds())) {
        filters.add(new Filter("vpc-id", instanceFilter.getVpcIds()));
      }
      if (isNotEmpty(instanceFilter.getSecurityGroupIds())) {
        filters.add(new Filter("instance.group-id", instanceFilter.getSecurityGroupIds()));
      }
      if (isNotEmpty(instanceFilter.getSubnetIds())) {
        filters.add(new Filter("network-interface.subnet-id", instanceFilter.getSubnetIds()));
      }
      if (isNotEmpty(instanceFilter.getTags())) {
        Multimap<String, String> tags = ArrayListMultimap.create();
        instanceFilter.getTags().forEach(tag -> tags.put(tag.getKey(), tag.getValue()));
        tags.keySet().forEach(key -> filters.add(new Filter("tag:" + key, new ArrayList<>(tags.get(key)))));
      }
      if (StringUtils.equals(awsInfrastructureMapping.getDeploymentType(), DeploymentType.WINRM.toString())) {
        filters.add(new Filter("platform", asList("windows")));
      }
    }
    return filters;
  }
}

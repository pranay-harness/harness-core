package software.wings.sm.states;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import org.apache.commons.lang.StringUtils;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.stencils.DataProvider;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by rishi on 2/9/17.
 */

@Singleton
public class LoadBalancerDataProvider implements DataProvider {
  @Inject private SettingsService settingsService;
  @Inject private AwsHelperService awsHelperService;
  @Inject private InfrastructureMappingService infrastructureMappingService;

  @Override
  public Map<String, String> getData(String appId, String... params) {
    AmazonElasticLoadBalancingClient amazonElasticLoadBalancingClient =
        awsHelperService.getAmazonElasticLoadBalancingClient(
            "AKIAIJ5H5UG5TUB3L2QQ", "Yef4E+CZTR2wRQc3IVfDS4Ls22BAeab9JVlZx2nu");

    return amazonElasticLoadBalancingClient.describeLoadBalancers(new DescribeLoadBalancersRequest().withPageSize(400))
        .getLoadBalancers()
        .stream()
        .filter(loadBalancer -> StringUtils.equalsIgnoreCase(loadBalancer.getType(), "classic"))
        .map(LoadBalancer::getLoadBalancerName)
        .collect(Collectors.toMap(Function.identity(), Function.identity()));
  }
}

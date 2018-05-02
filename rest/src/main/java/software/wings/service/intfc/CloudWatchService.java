package software.wings.service.intfc;

import software.wings.service.impl.cloudwatch.AwsNameSpace;
import software.wings.service.impl.cloudwatch.CloudWatchMetric;

import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 12/14/16.
 */
public interface CloudWatchService {
  /**
   * List namespaces list.
   *
   * @param settingId the setting id
   * @param region    the region
   * @return the list
   */
  List<String> listNamespaces(String settingId, String region);

  /**
   * List metrics list.
   *
   * @param settingId the setting id
   * @param region    the region
   * @param namespace the namespace
   * @return the list
   */
  List<String> listMetrics(String settingId, String region, String namespace);

  /**
   * List dimensions list.
   *
   * @param settingId  the setting id
   * @param region     the region
   * @param namespace  the namespace
   * @param metricName the metric name
   * @return the list
   */
  List<String> listDimensions(String settingId, String region, String namespace, String metricName);

  Map<AwsNameSpace, List<CloudWatchMetric>> getCloudWatchMetrics();
}

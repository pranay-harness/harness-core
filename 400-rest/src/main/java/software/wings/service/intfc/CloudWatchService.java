/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc;

import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.cloudwatch.AwsNameSpace;
import software.wings.service.impl.cloudwatch.CloudWatchMetric;
import software.wings.service.impl.cloudwatch.CloudWatchSetupTestNodeData;

import com.amazonaws.services.ecs.model.Service;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

  Set<String> getLoadBalancerNames(String settingId, String region);

  List<String> getLambdaFunctionsNames(String settingId, String region);

  /**
   * Api to fetch metric data for given node.
   * @param setupTestNodeData
   * @return
   */
  VerificationNodeDataSetupResponse getMetricsWithDataForNode(CloudWatchSetupTestNodeData setupTestNodeData);

  Map<String, List<CloudWatchMetric>> createLambdaFunctionNames(List<String> lambdaFunctions);

  Map<String, String> getGroupNameByHost(List<String> ec2InstanceNames);

  Map<String, String> getEC2Instances(String settingId, String region);

  List<String> getECSClusterNames(String settingId, String region);

  List<Service> getECSClusterServices(String settingId, String region, String clusterName);

  void setStatisticsAndUnit(AwsNameSpace awsNameSpace, List<CloudWatchMetric> metrics);
}

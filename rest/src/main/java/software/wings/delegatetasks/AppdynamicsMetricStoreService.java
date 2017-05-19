package software.wings.delegatetasks;

import software.wings.service.impl.appdynamics.AppdynamicsMetricData;

import java.util.List;

/**
 * Created by rsingh on 5/18/17.
 */
public interface AppdynamicsMetricStoreService { void save(String accountId, List<AppdynamicsMetricData> metricData); }

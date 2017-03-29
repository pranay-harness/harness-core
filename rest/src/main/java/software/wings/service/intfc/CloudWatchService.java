package software.wings.service.intfc;

import java.util.List;

/**
 * Created by anubhaw on 12/14/16.
 */
public interface CloudWatchService {
  /**
   * List namespaces listStateMachines.
   *
   * @param settingId the setting id
   * @return the listStateMachines
   */
  List<String> listNamespaces(String settingId);

  /**
   * List metrics listStateMachines.
   *
   * @param settingId the setting id
   * @param namespace the namespace
   * @return the listStateMachines
   */
  List<String> listMetrics(String settingId, String namespace);

  /**
   * List dimensions listStateMachines.
   *
   * @param settingId  the setting id
   * @param namespace  the namespace
   * @param metricName the metric name
   * @return the listStateMachines
   */
  List<String> listDimensions(String settingId, String namespace, String metricName);
}

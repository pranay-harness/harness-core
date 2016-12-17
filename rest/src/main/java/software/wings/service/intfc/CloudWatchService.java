package software.wings.service.intfc;

import java.util.List;

/**
 * Created by anubhaw on 12/14/16.
 */
public interface CloudWatchService {
  /**
   * List namespaces list.
   *
   * @param settingId the setting id
   * @return the list
   */
  List<String> listNamespaces(String settingId);

  /**
   * List metrics list.
   *
   * @param settingId the setting id
   * @param namespace the namespace
   * @return the list
   */
  List<String> listMetrics(String settingId, String namespace);

  /**
   * List dimensions list.
   *
   * @param settingId  the setting id
   * @param namespace  the namespace
   * @param metricName the metric name
   * @return the list
   */
  List<String> listDimensions(String settingId, String namespace, String metricName);
}

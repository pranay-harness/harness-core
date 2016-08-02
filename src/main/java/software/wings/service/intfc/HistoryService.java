package software.wings.service.intfc;

import software.wings.beans.History;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

/**
 * Created by peeyushaggarwal on 6/20/16.
 */
public interface HistoryService {
  /**
   * Create.
   *
   * @param history the history
   */
  void create(History history);

  /**
   * List page response.
   *
   * @param request the request
   * @return the page response
   */
  PageResponse<History> list(PageRequest<History> request);

  /**
   * Get history.
   *
   * @param appId     the app id
   * @param historyId the history id
   * @return history
   */
  History get(String appId, String historyId);
}

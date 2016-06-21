package software.wings.service.intfc;

import software.wings.beans.History;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

/**
 * Created by peeyushaggarwal on 6/20/16.
 */
public interface HistoryService {
  void create(History history);
  PageResponse<History> list(PageRequest<History> request);
}

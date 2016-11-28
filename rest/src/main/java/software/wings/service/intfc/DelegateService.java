package software.wings.service.intfc;

import software.wings.beans.Delegate;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

/**
 * Created by peeyushaggarwal on 11/28/16.
 */
public interface DelegateService {
  PageResponse<Delegate> list(PageRequest<Delegate> pageRequest);
  Delegate get(String accountId, String delegateId);
  Delegate update(Delegate delegate);
  Delegate add(Delegate delegate);
  void delete(String accountId, String delegateId);
}

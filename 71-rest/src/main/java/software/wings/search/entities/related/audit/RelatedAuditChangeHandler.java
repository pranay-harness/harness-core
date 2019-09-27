package software.wings.search.entities.related.audit;

import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.changestreams.ChangeEvent;

public class RelatedAuditChangeHandler implements ChangeHandler {
  public boolean handleChange(ChangeEvent changeEvent) {
    return true;
  }
}

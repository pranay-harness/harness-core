package software.wings.helpers.ext;

import java.util.List;

/**
 * Created by peeyushaggarwal on 5/12/16.
 */
public class SvnChangeSet {
  private List<SvnRevision> revisions;

  public List<SvnRevision> getRevisions() {
    return revisions;
  }

  public void setRevisions(List<SvnRevision> revisions) {
    this.revisions = revisions;
  }
}

package software.wings.helpers.ext.jenkins;

import java.util.List;

/**
 * Created by peeyushaggarwal on 5/12/16.
 */
public class SvnChangeSet {
  private List<SvnRevision> revisions;

  /**
   * Gets revisions.
   *
   * @return the revisions
   */
  public List<SvnRevision> getRevisions() {
    return revisions;
  }

  /**
   * Sets revisions.
   *
   * @param revisions the revisions
   */
  public void setRevisions(List<SvnRevision> revisions) {
    this.revisions = revisions;
  }
}

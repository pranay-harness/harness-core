package software.wings.helpers.ext.jenkins;

import com.offbytwo.jenkins.model.BaseModel;

/**
 * Created by peeyushaggarwal on 5/12/16.
 */
public class SvnBuildDetails extends BaseModel {
  private SvnChangeSet changeSet;

  /**
   * Gets change set.
   *
   * @return the change set
   */
  public SvnChangeSet getChangeSet() {
    return changeSet;
  }

  /**
   * Sets change set.
   *
   * @param changeSet the change set
   */
  public void setChangeSet(SvnChangeSet changeSet) {
    this.changeSet = changeSet;
  }
}

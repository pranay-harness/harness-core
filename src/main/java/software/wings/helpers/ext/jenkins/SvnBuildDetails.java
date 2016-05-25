package software.wings.helpers.ext.jenkins;

import com.offbytwo.jenkins.model.BaseModel;

/**
 * Created by peeyushaggarwal on 5/12/16.
 */
public class SvnBuildDetails extends BaseModel {
  private SvnChangeSet changeSet;

  public SvnChangeSet getChangeSet() {
    return changeSet;
  }

  public void setChangeSet(SvnChangeSet changeSet) {
    this.changeSet = changeSet;
  }
}

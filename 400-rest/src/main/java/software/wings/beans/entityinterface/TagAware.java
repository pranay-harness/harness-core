package software.wings.beans.entityinterface;

import software.wings.beans.HarnessTagLink;

import java.util.List;

public interface TagAware {
  List<HarnessTagLink> getTagLinks();
  void setTagLinks(List<HarnessTagLink> tagLinks);
}

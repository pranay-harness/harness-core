package software.wings.beans.entityinterface;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.HarnessTagLink;

import java.util.List;

@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._957_CG_BEANS)
public interface TagAware {
  List<HarnessTagLink> getTagLinks();

  void setTagLinks(List<HarnessTagLink> tagLinks);
}

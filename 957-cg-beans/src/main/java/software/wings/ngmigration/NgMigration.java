package software.wings.ngmigration;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface NgMigration {
  DiscoveryNode discover(NGMigrationEntity entity);

  DiscoveryNode discover(String accountId, String appId, String entityId);

  NGMigrationStatus canMigrate(
      Map<CgEntityId, CgEntityNode> entities, Map<CgEntityId, Set<CgEntityId>> graph, CgEntityId entityId);

  void migrate(Map<CgEntityId, CgEntityNode> entities, Map<CgEntityId, Set<CgEntityId>> graph, CgEntityId entityId);

  List<NGYamlFile> getYamls(
      Map<CgEntityId, CgEntityNode> entities, Map<CgEntityId, Set<CgEntityId>> graph, CgEntityId entityId);
}

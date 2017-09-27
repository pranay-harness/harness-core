package software.wings.service.intfc.yaml;

import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.ServiceCommand;
import software.wings.yaml.gitSync.EntityUpdateEvent;
import software.wings.yaml.gitSync.EntityUpdateEvent.SourceType;
import software.wings.yaml.gitSync.EntityUpdateListEvent;

/**
 * Entity Update Service.
 *
 * @author bsollish
 */
public interface EntityUpdateService {
  public void queueEntityUpdateList(EntityUpdateListEvent entityUpdateListEvent);

  public EntityUpdateEvent setupListUpdate(Account account, SourceType sourceType);

  public EntityUpdateEvent appListUpdate(Application app, SourceType sourceType);

  public EntityUpdateEvent serviceListUpdate(Service service, SourceType sourceType);

  public EntityUpdateEvent serviceCommandListUpdate(ServiceCommand serviceCommand, SourceType sourceType);

  public EntityUpdateEvent environmentListUpdate(Environment environment, SourceType sourceType);

  public EntityUpdateEvent workflowListUpdate(Workflow workflow, SourceType sourceType);

  public EntityUpdateEvent pipelineListUpdate(Pipeline pipeline, SourceType sourceType);

  public EntityUpdateEvent triggerListUpdate(ArtifactStream artifactStream, SourceType sourceType);

  public EntityUpdateEvent settingAttributeListUpdate(SettingAttribute settingAttribute, SourceType sourceType);
}

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
  public void setupUpdate(Account account, SourceType sourceType);

  public void appUpdate(Application app, SourceType sourceType);

  public void serviceUpdate(Service service, SourceType sourceType);

  public void serviceCommandUpdate(ServiceCommand serviceCommand, SourceType sourceType);

  public void environmentUpdate(Environment environment, SourceType sourceType);

  public void workflowUpdate(Workflow workflow, SourceType sourceType);

  public void pipelineUpdate(Pipeline pipeline, SourceType sourceType);

  public void triggerUpdate(ArtifactStream artfifactStream, SourceType sourceType);

  public void settingAttributeUpdate(SettingAttribute settingAttribute, SourceType sourceType);

  //------------------------

  public void queueEntityUpdateList(EntityUpdateListEvent entityUpdateListEvent);

  public EntityUpdateEvent appListUpdate(Application app, SourceType sourceType);

  public EntityUpdateEvent serviceListUpdate(Service service, SourceType sourceType);
}

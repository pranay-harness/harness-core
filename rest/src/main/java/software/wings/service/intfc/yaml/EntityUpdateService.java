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

/**
 * Entity Update Service.
 *
 * @author bsollish
 */
public interface EntityUpdateService {
  public void setupUpdate(Account account);

  public void appUpdate(Application app);

  public void serviceUpdate(Service service);

  public void serviceCommandUpdate(ServiceCommand serviceCommand);

  public void environmentUpdate(Environment environment);

  public void workflowUpdate(Workflow workflow);

  public void pipelineUpdate(Pipeline pipeline);

  public void triggerUpdate(ArtifactStream artfifactStream);

  public void settingAttributeUpdate(SettingAttribute settingAttribute);
}

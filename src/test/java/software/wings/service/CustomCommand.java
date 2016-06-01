package software.wings.service;

import static software.wings.beans.ExecCommandUnit.ExecCommandUnitBuilder.anExecCommandUnit;
import static software.wings.service.intfc.FileService.FileBucket.PLATFORMS;

import software.wings.beans.CommandUnit;
import software.wings.beans.ExecCommandUnit;
import software.wings.beans.Execution;
import software.wings.beans.Host;
import software.wings.beans.ScpCommandUnit;
import software.wings.beans.ScpCommandUnit.ScpCommandUnitBuilder;
import software.wings.beans.User;

import java.util.Arrays;
import java.util.List;

/**
 * Created by anubhaw on 5/24/16.
 */
public class CustomCommand extends Execution {
  @Override
  public List<CommandUnit> getCommandUnits() {
    ExecCommandUnit setup =
        anExecCommandUnit().withCommandString("rm -rf wings && mkdir -p $HOME/wings/downloads").build();
    ScpCommandUnit scpCommandUnit = ScpCommandUnitBuilder.aScpCommandUnit()
                                        .withDestinationFilePath("$HOME")
                                        .withFileBucket(PLATFORMS)
                                        .withFileId("574ddc4d8a31bba72592ae0a")
                                        .build();
    ExecCommandUnit run = anExecCommandUnit().withCommandString("sh $HOME/startup.sh").build();
    return Arrays.asList(setup, scpCommandUnit, run);
  }

  public static final class CustomCommandBuilder {
    private Host host;
    private String sshUser;
    private String sshPassword;
    private String appAccount;
    private String appAccountPassword;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private CustomCommandBuilder() {}

    public static CustomCommandBuilder aCustomCommand() {
      return new CustomCommandBuilder();
    }

    public CustomCommandBuilder withHost(Host host) {
      this.host = host;
      return this;
    }

    public CustomCommandBuilder withSshUser(String sshUser) {
      this.sshUser = sshUser;
      return this;
    }

    public CustomCommandBuilder withSshPassword(String sshPassword) {
      this.sshPassword = sshPassword;
      return this;
    }

    public CustomCommandBuilder withAppAccount(String appAccount) {
      this.appAccount = appAccount;
      return this;
    }

    public CustomCommandBuilder withAppAccountPassword(String appAccountPassword) {
      this.appAccountPassword = appAccountPassword;
      return this;
    }

    public CustomCommandBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public CustomCommandBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public CustomCommandBuilder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public CustomCommandBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public CustomCommandBuilder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public CustomCommandBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public CustomCommandBuilder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public CustomCommandBuilder but() {
      return aCustomCommand()
          .withHost(host)
          .withSshUser(sshUser)
          .withSshPassword(sshPassword)
          .withAppAccount(appAccount)
          .withAppAccountPassword(appAccountPassword)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    public CustomCommand build() {
      CustomCommand customCommand = new CustomCommand();
      customCommand.setHost(host);
      customCommand.setSshUser(sshUser);
      customCommand.setSshPassword(sshPassword);
      customCommand.setAppAccount(appAccount);
      customCommand.setAppAccountPassword(appAccountPassword);
      customCommand.setUuid(uuid);
      customCommand.setAppId(appId);
      customCommand.setCreatedBy(createdBy);
      customCommand.setCreatedAt(createdAt);
      customCommand.setLastUpdatedBy(lastUpdatedBy);
      customCommand.setLastUpdatedAt(lastUpdatedAt);
      customCommand.setActive(active);
      return customCommand;
    }
  }
}

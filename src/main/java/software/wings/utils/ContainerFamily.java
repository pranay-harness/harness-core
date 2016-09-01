package software.wings.utils;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Collections.singletonList;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.command.CommandUnitType.COMMAND;
import static software.wings.beans.command.CommandUnitType.COPY_CONFIGS;
import static software.wings.beans.command.CommandUnitType.EXEC;
import static software.wings.beans.command.CommandUnitType.PORT_CHECK_CLEARED;
import static software.wings.beans.command.CommandUnitType.PORT_CHECK_LISTENING;
import static software.wings.beans.command.CommandUnitType.PROCESS_CHECK_RUNNING;
import static software.wings.beans.command.CommandUnitType.PROCESS_CHECK_STOPPED;
import static software.wings.beans.command.CommandUnitType.SCP;
import static software.wings.beans.command.CommandUnitType.SETUP_ENV;
import static software.wings.common.UUIDGenerator.graphIdGenerator;

import com.google.common.collect.Lists;

import software.wings.beans.Graph;
import software.wings.beans.command.ScpCommandUnit.ScpFileCategory;

import java.util.List;

/**
 * Created by peeyushaggarwal on 8/31/16.
 */
public enum ContainerFamily {
  TOMCAT {
    private static final long serialVersionUID = 2932493038229748527L;

    @Override
    protected Graph getStartCommandGraph(ArtifactType artifactType) {
      return aGraph()
          .withGraphName("Start")
          .addNodes(aNode()
                        .withOrigin(true)
                        .withX(50)
                        .withY(50)
                        .withId(graphIdGenerator("node"))
                        .withType(EXEC.name())
                        .withName("Start Service")
                        .addProperty("commandPath", "$WINGS_RUNTIME_PATH/tomcat/bin")
                        .addProperty("commandString",
                            "export CATALINA_OPTS=\"$CATALINA_OPTS -javaagent:$HOME/appagent/javaagent.jar\"\n"
                                + "./startup.sh")
                        .addProperty("tailFiles", true)
                        .addProperty("tailPatterns",
                            singletonList(of("filePath", "$WINGS_RUNTIME_PATH/tomcat/logs/catalina.out", "pattern",
                                "Server startup in")))
                        .build(),
              aNode()
                  .withX(200)
                  .withY(50)
                  .withId(graphIdGenerator("node"))
                  .withName("Process Running")
                  .withType(PROCESS_CHECK_RUNNING.name())
                  .addProperty("commandString", "set -x\npgrep -f \"\\-Dcatalina.home=$WINGS_RUNTIME_PATH/tomcat\"")
                  .build(),
              aNode()
                  .withX(350)
                  .withY(50)
                  .withId(graphIdGenerator("node"))
                  .withType(PORT_CHECK_LISTENING.name())
                  .withName("Port Listening")
                  .addProperty("commandString",
                      "set -x\n"
                          + "server_xml=\"$WINGS_RUNTIME_PATH/tomcat/conf/server.xml\"\n"
                          + "\n"
                          + "if [ -f \"$server_xml\" ]\n"
                          + "then\n"
                          + "port=$(grep \"<Connector[ ]*port=\\\"[0-9]*\\\"[ ]*protocol=\\\"HTTP/1.1\\\"\" \"$server_xml\" |cut -d '\"' -f2)\n"
                          + "nc -v -z -w 5 localhost $port\n"
                          + "else\n"
                          + " echo \"Tomcat config file(\"$server_xml\") does not exist.. port check failed.\"\n"
                          + " exit 1\n"
                          + "fi")
                  .build())
          .buildPipeline();
    }

    @Override
    protected Graph getStopCommandGraph(ArtifactType artifactType) {
      return aGraph()
          .withGraphName("Stop")
          .addNodes(aNode()
                        .withOrigin(true)
                        .withX(50)
                        .withY(50)
                        .withId(graphIdGenerator("node"))
                        .withType(EXEC.name())
                        .withName("Stop Service")
                        .addProperty("commandPath", "$WINGS_RUNTIME_PATH/tomcat/bin")
                        .addProperty("commandString", "[[ -f ./shutdown.sh ]] && ./shutdown.sh  || true")
                        .build(),
              aNode()
                  .withX(200)
                  .withY(50)
                  .withId(graphIdGenerator("node"))
                  .withName("Process Stopped")
                  .withType(PROCESS_CHECK_STOPPED.name())
                  .addProperty("commandString",
                      "set -x\npgrep -f \"\\-Dcatalina.home=$WINGS_RUNTIME_PATH/tomcat\"\nrc=$?\nif [ \"$rc\" -eq 0 ]\nthen\nexit 1\nfi")
                  .build(),
              aNode()
                  .withX(350)
                  .withY(50)
                  .withId(graphIdGenerator("node"))
                  .withType(PORT_CHECK_CLEARED.name())
                  .withName("Port Cleared")
                  .addProperty("commandString",
                      "set -x\n"
                          + "server_xml=\"$WINGS_RUNTIME_PATH/tomcat/conf/server.xml\"\n"
                          + "if [ -f \"$server_xml\" ]\n"
                          + "then\n"
                          + "port=$(grep \"<Connector[ ]*port=\\\"[0-9]*\\\"[ ]*protocol=\\\"HTTP/1.1\\\"\" \"$server_xml\" |cut -d '\"' -f2)\n"
                          + "nc -v -z -w 5 localhost $port\n"
                          + "rc=$?\n"
                          + "if [ \"$rc\" -eq 0 ]\n"
                          + "then\n"
                          + "exit 1\n"
                          + "fi\n"
                          + "else\n"
                          + " echo \"Tomcat config file(\"$server_xml\") does not exist.. skipping port check.\"\n"
                          + "fi")
                  .build())
          .buildPipeline();
    }

    @Override
    protected Graph getInstallCommandGraph(ArtifactType artifactType) {
      return aGraph()
          .withGraphName("Install")
          .addNodes(
              aNode()
                  .withOrigin(true)
                  .withX(50)
                  .withY(50)
                  .withId(graphIdGenerator("node"))
                  .withName("Setup Runtime Paths")
                  .withType(SETUP_ENV.name())
                  .addProperty("commandString",
                      "mkdir -p \"$WINGS_RUNTIME_PATH\"\nmkdir -p \"$WINGS_BACKUP_PATH\"\nmkdir -p \"$WINGS_STAGING_PATH\"")
                  .build(),
              aNode()
                  .withX(200)
                  .withY(50)
                  .withId(graphIdGenerator("node"))
                  .withName("Stop")
                  .withType(COMMAND.name())
                  .addProperty("referenceId", "Stop")
                  .build(),
              aNode()
                  .withX(350)
                  .withY(50)
                  .withId(graphIdGenerator("node"))
                  .withName("Copy App Stack")
                  .withType(SCP.name())
                  .addProperty("destinationDirectoryPath", "$WINGS_RUNTIME_PATH")
                  .addProperty("fileCategory", ScpFileCategory.APPLICATION_STACK)
                  .build(),
              aNode()
                  .withX(500)
                  .withY(50)
                  .withId(graphIdGenerator("node"))
                  .withName("Expand App Server")
                  .withType(EXEC.name())
                  .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                  .addProperty("commandString",
                      "rm -rf tomcat\ntar -xvzf apache-tomcat-7.0.70.tar.gz\nmv apache-tomcat-7.0.70 tomcat")
                  .build(),
              aNode()
                  .withX(650)
                  .withY(50)
                  .withId(graphIdGenerator("node"))
                  .withName("Copy Artifact")
                  .withType(SCP.name())
                  .addProperty("fileCategory", ScpFileCategory.ARTIFACTS)
                  .addProperty("destinationDirectoryPath", "$WINGS_RUNTIME_PATH/tomcat/webapps")
                  .build(),
              aNode()
                  .withX(800)
                  .withY(50)
                  .withId(graphIdGenerator("node"))
                  .withName("Copy Configs")
                  .withType(COPY_CONFIGS.name())
                  .addProperty("destinationParentPath", "$WINGS_RUNTIME_PATH")
                  .build(),
              aNode()
                  .withX(950)
                  .withY(50)
                  .withId(graphIdGenerator("node"))
                  .withName("Start")
                  .withType(COMMAND.name())
                  .addProperty("referenceId", "Start")
                  .build())
          .buildPipeline();
    }
  };

  public List<Graph> getDefaultCommands(ArtifactType artifactType) {
    return Lists.newArrayList(
        getStartCommandGraph(artifactType), getInstallCommandGraph(artifactType), getStopCommandGraph(artifactType));
  }

  protected abstract Graph getStopCommandGraph(ArtifactType artifactType);

  protected abstract Graph getStartCommandGraph(ArtifactType artifactType);

  protected abstract Graph getInstallCommandGraph(ArtifactType artifactType);
}

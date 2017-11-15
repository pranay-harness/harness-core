package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.ContainerOrchestrationCommandUnit;

/**
 * @author rktummala on 11/13/17
 */
public abstract class ContainerCommandUnitYamlHandler<Y extends ContainerOrchestrationCommandUnit.Yaml, C
                                                          extends ContainerOrchestrationCommandUnit, B
                                                          extends ContainerOrchestrationCommandUnit.Yaml.Builder>
    extends CommandUnitYamlHandler<Y, C, B> {}

package software.wings.graphql.datafetcher;

import lombok.Getter;

import java.util.EnumSet;

public enum RuntimeConnectionWiringEnum {
  WORKFLOW_CONNECTION(DataFetcherEnum.WORKFLOW, EnumSet.of(RuntimeWiringNameEnum.QUERY)),
  WORKFLOWS_CONNECTION(DataFetcherEnum.WORKFLOWS, EnumSet.of(RuntimeWiringNameEnum.QUERY)),
  WORKFLOW_EXECUTION_CONNECTION(DataFetcherEnum.WORKFLOW_EXECUTION, EnumSet.of(RuntimeWiringNameEnum.QUERY)),
  WORKFLOW_EXECUTIONS_CONNECTION(DataFetcherEnum.WORKFLOW_EXECUTIONS, EnumSet.of(RuntimeWiringNameEnum.QUERY)),
  ARTIFACT_CONNECTION(DataFetcherEnum.DEPLOYED_ARTIFACTS, EnumSet.of(RuntimeWiringNameEnum.QUERY)),
  APPLICATION_CONNECTION(DataFetcherEnum.APPLICATION, EnumSet.of(RuntimeWiringNameEnum.QUERY)),
  APPLICATIONS_CONNECTION(DataFetcherEnum.APPLICATIONS, EnumSet.of(RuntimeWiringNameEnum.QUERY)),
  ENVIRONMENT_CONNECTION(
      DataFetcherEnum.ENVIRONMENT, EnumSet.of(RuntimeWiringNameEnum.QUERY, RuntimeWiringNameEnum.WORKFLOW)),
  ENVIRONMENTS_CONNECTION(DataFetcherEnum.ENVIRONMENTS, EnumSet.of(RuntimeWiringNameEnum.QUERY));

  @Getter private DataFetcherEnum dataFetcherEnum;
  @Getter private EnumSet<RuntimeWiringNameEnum> runtimeWiringNameEnums;

  RuntimeConnectionWiringEnum(DataFetcherEnum dataFetcherEnum, EnumSet<RuntimeWiringNameEnum> runtimeWiringNameEnums) {
    this.dataFetcherEnum = dataFetcherEnum;
    this.runtimeWiringNameEnums = runtimeWiringNameEnums;
  }
}

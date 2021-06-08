package software.wings.delegatetasks.ondemand;

//@Data
//@Builder
//@NoArgsConstructor
//@FieldNameConstants(innerTypeName = "LearningEngineTaskKeys")
//@AllArgsConstructor
//@EqualsAndHashCode(callSuper = false)
//@JsonIgnoreProperties(ignoreUnknown = true)
//@Entity(value = "learningEngineTasks")
public class OnDemandDelegateTask {
  String accountId;
  OnDemandTaskType type;
  String kubeConfig;
  String delegateYaml;

  public static enum OnDemandTaskType { CREATE, DESTROY }
}

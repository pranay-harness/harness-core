package io.harness.beans.common;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@OwnedBy(PIPELINE)
// TODO this should go to yaml commons
@TargetModule(HarnessModule._884_PMS_COMMONS)
public interface SwaggerConstants {
  String STRING_CLASSPATH = "java.lang.String";
  String INTEGER_CLASSPATH = "java.lang.Integer";
  String DOUBLE_CLASSPATH = "java.lang.Double";
  String BOOLEAN_CLASSPATH = "java.lang.Boolean";
  String STRING_LIST_CLASSPATH = "[Ljava.lang.String;";
  String STRING_MAP_CLASSPATH = "Map[String,String]";
}

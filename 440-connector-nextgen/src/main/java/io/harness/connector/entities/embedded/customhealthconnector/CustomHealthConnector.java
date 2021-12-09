package io.harness.connector.entities.embedded.customhealthconnector;
import io.harness.connector.entities.Connector;
import io.harness.delegate.beans.connector.customhealthconnector.CustomHealthMethod;

import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@EqualsAndHashCode(callSuper = true)
@Entity(value = "connectors", noClassnameStored = true)
@Persistent
@TypeAlias("io.harness.connector.entities.embedded.customhealthconnector.CustomHealthConnector")
public class CustomHealthConnector extends Connector {
  String baseURL;
  List<CustomHealthConnectorKeyAndValue> headers;
  List<CustomHealthConnectorKeyAndValue> params;
  CustomHealthMethod method;
  String validationBody;
  String validationPath;
}

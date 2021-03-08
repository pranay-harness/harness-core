package io.harness.connector.entities.embedded.ceawsconnector;

import io.harness.connector.entities.Connector;
import io.harness.delegate.beans.connector.awsconnector.CrossAccountAccessDTO;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsFeatures;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@FieldNameConstants(innerTypeName = "CEAwsConfigKeys")
@EqualsAndHashCode(callSuper = false)
@Entity(value = "connectors", noClassnameStored = true)
@Persistent
@TypeAlias("io.harness.connector.entities.embedded.ceawsconnector.CEAwsConfig")
public class CEAwsConfig extends Connector {
  List<CEAwsFeatures> featuresEnabled;
  String awsAccountId;
  CURAttributes curAttributes;
  CrossAccountAccessDTO crossAccountAccess;
}

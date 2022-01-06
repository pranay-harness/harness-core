/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.servicenow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.delegatetasks.servicenow.ServiceNowAction;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDC)
public class ServiceNowCreateUpdateParams {
  @Getter @Setter private ServiceNowAction action;
  @Getter @Setter String snowConnectorName;
  @Getter @Setter String snowConnectorId;
  @Getter @Setter private String ticketType;
  @Setter private Map<ServiceNowFields, String> fields;
  @Setter private Map<String, String> additionalFields;
  @Getter @Setter private String issueNumber;
  @Getter @Setter private String ticketId;
  @Getter @Setter private boolean updateMultiple;
  // Import set fields
  @Getter @Setter private String importSetTableName;
  @Getter @Setter private String jsonBody;

  public Map<ServiceNowFields, String> fetchFields() {
    return fields;
  }
  public Map<String, String> fetchAdditionalFields() {
    return additionalFields;
  }
}

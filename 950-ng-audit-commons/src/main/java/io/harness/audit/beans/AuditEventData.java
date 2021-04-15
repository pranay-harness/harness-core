package io.harness.audit.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.beans.custom.AuditEventDataTypeConstants.USER_INVITE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.custom.user.UserInviteAuditEventData;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

@OwnedBy(PL)
@Data
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(value = { @JsonSubTypes.Type(value = UserInviteAuditEventData.class, name = USER_INVITE) })
public abstract class AuditEventData {
  @NotNull @NotBlank protected String type;
}

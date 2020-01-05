package software.wings.beans;

import static software.wings.beans.PreferenceType.AUDIT_PREFERENCE;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;

import java.util.List;

/**
 * Audit Preference model
 */

@Data
@JsonTypeName("AUDIT_PREFERENCE")
@FieldNameConstants(innerTypeName = "AuditPreferenceKeys")
@EqualsAndHashCode(callSuper = true)
public class AuditPreference extends Preference {
  String startTime;
  String endTime;
  Integer lastNDays;
  // userIds who initiated action captured by audit records
  List<String> createdByUserIds;
  List<String> operationTypes;
  boolean includeAccountLevelResources;
  boolean includeAppLevelResources;
  AccountAuditFilter accountAuditFilter;
  ApplicationAuditFilter applicationAuditFilter;

  public AuditPreference() {
    super(AUDIT_PREFERENCE.name());
  }

  @UtilityClass
  public static final class AuditPreferenceKeys {
    public static final String accountId = "accountId";
    public static final String createdAt = "createdAt";
    public static final String uuid = "uuid";
    public static final String name = "name";
    public static final String preferenceType = "preferenceType";
    public static final String userId = "userId";
  }
}

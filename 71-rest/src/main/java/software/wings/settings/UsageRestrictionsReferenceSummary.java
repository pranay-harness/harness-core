package software.wings.settings;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

/**
 * This class is used to report the reference summary/statistics of one specified App/Env by usage restrictions of
 * account-level settings/secrets. So that before deleting an App/Env, this information is provided to the end-user
 * to decide if they would like to delete the specified App/Env AND then cascading delete all the references in
 * previously defined usage restrictions.
 *
 * @author marklu on 11/1/18
 */
@Data
@Builder
public class UsageRestrictionsReferenceSummary {
  private int total;
  private int numOfSettings;
  private int numOfSecrets;
  private Set<IdNameReference> settings;
  private Set<IdNameReference> secrets;

  @Data
  @Builder
  @EqualsAndHashCode
  public static class IdNameReference {
    private String id;
    private String name;
  }
}

package software.wings.beans;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhaw on 3/6/17.
 */
@Entity(value = "userInvites", noClassnameStored = true)
//@Indexes(@Index(fields = {@Field("accountId"), @Field("email")}, options = @IndexOptions(unique = true)))
public class UserInvite extends Base {
  @NotEmpty private String accountId;
  @NotEmpty private String email;
  @Reference(idOnly = true, ignoreMissing = true) private List<Role> roles = new ArrayList<>();
  private boolean complete = false;

  /**
   * Gets account id.
   *
   * @return the account id
   */
  public String getAccountId() {
    return accountId;
  }

  /**
   * Sets account id.
   *
   * @param accountId the account id
   */
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  /**
   * Gets email.
   *
   * @return the email
   */
  public String getEmail() {
    return email;
  }

  /**
   * Sets email.
   *
   * @param email the email
   */
  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * Gets roles.
   *
   * @return the roles
   */
  public List<Role> getRoles() {
    return roles;
  }

  /**
   * Sets roles.
   *
   * @param roles the roles
   */
  public void setRoles(List<Role> roles) {
    this.roles = roles;
  }

  /**
   * Is complete boolean.
   *
   * @return the boolean
   */
  public boolean isComplete() {
    return complete;
  }

  /**
   * Sets complete.
   *
   * @param complete the complete
   */
  public void setComplete(boolean complete) {
    this.complete = complete;
  }
}

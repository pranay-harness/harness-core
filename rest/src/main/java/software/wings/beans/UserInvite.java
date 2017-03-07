package software.wings.beans;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Transient;
import software.wings.utils.validation.Update;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhaw on 3/6/17.
 */
@Entity(value = "userInvites", noClassnameStored = true)
//@Indexes(@Index(fields = {@Field("accountId"), @Field("email")}, options = @IndexOptions(unique = true))) //TODO:
//handle update with insert and then uncomment
public class UserInvite extends Base {
  @NotEmpty private String accountId;
  @NotEmpty(groups = {Update.class}) private String email;
  @Reference(idOnly = true, ignoreMissing = true) private List<Role> roles = new ArrayList<>();
  private boolean completed = false;
  @Transient @JsonProperty(access = WRITE_ONLY) private List<String> emails = new ArrayList<>();

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
  public boolean isCompleted() {
    return completed;
  }

  /**
   * Sets complete.
   *
   * @param completed the complete
   */
  public void setCompleted(boolean completed) {
    this.completed = completed;
  }

  public List<String> getEmails() {
    return emails;
  }

  public void setEmails(List<String> emails) {
    this.emails = emails;
  }
}

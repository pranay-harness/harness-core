package software.wings.beans;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 *
 */
@Data
@Builder
public class AccountSalesContactsInfo {
  List<String> salesContacts;
}

package io.harness.accesscontrol.principals.serviceaccounts.persistence;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static org.springframework.data.mongodb.util.MongoDbErrorCodes.isDuplicateKeyCode;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.MongoBulkWriteException;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.BulkOperationException;
import org.springframework.data.mongodb.core.BulkOperations.BulkMode;
import org.springframework.data.mongodb.core.MongoTemplate;

@OwnedBy(HarnessTeam.PL)
@Singleton
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class ServiceAccountRepositoryCustomImpl implements ServiceAccountRepositoryCustom {
  private final MongoTemplate mongoTemplate;
  @Override
  public long insertAllIgnoringDuplicates(List<ServiceAccountDBO> serviceAccounts) {
    try {
      if (isEmpty(serviceAccounts)) {
        return 0;
      }
      return mongoTemplate.bulkOps(BulkMode.UNORDERED, ServiceAccountDBO.class)
          .insert(serviceAccounts)
          .execute()
          .getInsertedCount();
    } catch (BulkOperationException ex) {
      if (ex.getErrors().stream().allMatch(bulkWriteError -> isDuplicateKeyCode(bulkWriteError.getCode()))) {
        return ex.getResult().getInsertedCount();
      }
      throw ex;
    } catch (Exception ex) {
      if (ex.getCause() instanceof MongoBulkWriteException) {
        MongoBulkWriteException bulkWriteException = (MongoBulkWriteException) ex.getCause();
        if (bulkWriteException.getWriteErrors().stream().allMatch(
                writeError -> isDuplicateKeyCode(writeError.getCode()))) {
          return bulkWriteException.getWriteResult().getInsertedCount();
        }
      }
      throw ex;
    }
  }
}

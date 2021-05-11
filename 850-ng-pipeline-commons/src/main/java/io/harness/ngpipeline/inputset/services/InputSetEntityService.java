package io.harness.ngpipeline.inputset.services;

import io.harness.annotations.dev.ToBeDeleted;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@ToBeDeleted
@Deprecated
public interface InputSetEntityService {
  BaseInputSetEntity create(BaseInputSetEntity baseInputSetEntity);

  Optional<BaseInputSetEntity> get(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String inputSetIdentifier, boolean deleted);

  BaseInputSetEntity update(BaseInputSetEntity baseInputSetEntity);

  Page<BaseInputSetEntity> list(Criteria criteria, Pageable pageable);

  boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier,
      String inputSetIdentifier, Long version);
  void deleteInputSetsOfPipeline(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier);

  List<BaseInputSetEntity> getGivenInputSetList(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, Set<String> inputSetIdentifiersList);
}

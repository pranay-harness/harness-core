package io.harness.accesscontrol.acl.repository;

import io.harness.accesscontrol.acl.models.ACL;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

@NoRepositoryBean
public interface ACLBaseRepository<T extends ACL> extends PagingAndSortingRepository<T, String> {}

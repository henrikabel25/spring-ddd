package com.springddd.infrastructure.persistence.r2dbc;

import com.springddd.infrastructure.persistence.entity.SysUserRoleEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface SysUserRoleRepository extends ReactiveCrudRepository<SysUserRoleEntity, Long> {
}

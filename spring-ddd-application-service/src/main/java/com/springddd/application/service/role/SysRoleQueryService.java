package com.springddd.application.service.role;


import com.springddd.application.service.role.dto.SysRolePageQuery;
import com.springddd.application.service.role.dto.SysRoleQuery;
import com.springddd.application.service.role.dto.SysRoleView;
import com.springddd.application.service.role.dto.SysRoleViewMapStruct;
import com.springddd.domain.util.PageResponse;
import com.springddd.infrastructure.persistence.entity.SysRoleEntity;
import com.springddd.infrastructure.persistence.r2dbc.SysRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysRoleQueryService {

    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    private final SysRoleViewMapStruct sysRoleViewMapStruct;

    private final SysRoleRepository sysRoleRepository;

    public Mono<PageResponse<SysRoleView>> index(SysRolePageQuery query) {
        Criteria criteria = Criteria.where(SysRoleQuery.Fields.deleteStatus).is(false);
        if (!ObjectUtils.isEmpty(query.getRoleName())) {
            criteria = criteria.and(SysRoleQuery.Fields.roleName).like("%" + query.getRoleName() + "%");
        }
        if (!ObjectUtils.isEmpty(query.getRoleCode())) {
            criteria = criteria.and(SysRoleQuery.Fields.roleCode).like("%" + query.getRoleCode() + "%");
        }
        Query qry = Query.query(criteria)
                .limit(query.getPageSize())
                .offset((long) (query.getPageNum() - 1) * query.getPageSize());

        Mono<List<SysRoleView>> list = r2dbcEntityTemplate.select(SysRoleEntity.class).matching(qry).all().collectList().map(sysRoleViewMapStruct::toViewList);
        Mono<Long> count = r2dbcEntityTemplate.count(Query.query(criteria), SysRoleEntity.class);
        return Mono.zip(list, count)
                .map(tuple -> new PageResponse<>(tuple.getT1(), tuple.getT2(), query.getPageNum(), query.getPageSize()));
    }

    public Mono<PageResponse<SysRoleView>> recycle(SysRolePageQuery query) {
        Criteria criteria = Criteria.where(SysRoleQuery.Fields.deleteStatus).is(true);
        Query qry = Query.query(criteria)
                .limit(query.getPageSize())
                .offset((long) (query.getPageNum() - 1) * query.getPageSize());

        Mono<List<SysRoleView>> list = r2dbcEntityTemplate.select(SysRoleEntity.class).matching(qry).all().collectList().map(sysRoleViewMapStruct::toViewList);
        Mono<Long> count = r2dbcEntityTemplate.count(Query.query(criteria), SysRoleEntity.class);
        return Mono.zip(list, count)
                .map(tuple -> new PageResponse<>(tuple.getT1(), tuple.getT2(), query.getPageNum(), query.getPageSize()));
    }

    public Mono<SysRoleView> getById(Long id) {
        return r2dbcEntityTemplate.select(SysRoleEntity.class).matching(Query.query(
                Criteria.where(SysRoleQuery.Fields.id).is(id).and(SysRoleQuery.Fields.deleteStatus).is(false))).one().map(sysRoleViewMapStruct::toView);
    }

    public Mono<SysRoleView> getByCode(String code) {
        return r2dbcEntityTemplate.select(SysRoleEntity.class).matching(Query.query(Criteria
                .where(SysRoleQuery.Fields.roleCode).is(code)
                .and(SysRoleQuery.Fields.deleteStatus).is(false))).one().map(sysRoleViewMapStruct::toView);
    }

    public Mono<List<SysRoleView>> getAllRole() {
        return sysRoleRepository.findAll().collectList().map(sysRoleViewMapStruct::toViewList);
    }
}

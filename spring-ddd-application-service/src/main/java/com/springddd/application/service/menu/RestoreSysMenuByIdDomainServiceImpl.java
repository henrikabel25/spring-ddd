package com.springddd.application.service.menu;

import com.springddd.domain.menu.RestoreSysMenuByIdDomainService;
import com.springddd.infrastructure.cache.util.ReactiveRedisCacheHelper;
import com.springddd.infrastructure.persistence.entity.SysMenuEntity;
import com.springddd.infrastructure.persistence.entity.SysMenuTreeNodeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RestoreSysMenuByIdDomainServiceImpl implements RestoreSysMenuByIdDomainService {

    private static final int BATCH_SIZE = 1000;

    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    private final ReactiveRedisCacheHelper reactiveRedisCacheHelper;

    @Override
    public Mono<Void> restoreByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Mono.empty();
        }
        return r2dbcEntityTemplate.select(SysMenuTreeNodeEntity.class)
                .matching(Query.query(Criteria.where("delete_status").is(true)))
                .all()
                .collectList()
                .flatMap(deletedMenus -> {
                    List<Long> allIds = DeleteSysMenuByIdDomainServiceImpl.collectDescendantIds(deletedMenus, ids);
                    return updateDeleteStatusInBatches(allIds, false)
                            .then(reactiveRedisCacheHelper.deleteCache("user:*:menuWithPermissions"))
                            .then(reactiveRedisCacheHelper.deleteCache("user:*:menuWithoutPermissions"))
                            .then(reactiveRedisCacheHelper.deleteCache("auth:user:detail:*").onErrorResume(e -> Mono.empty()));
                });
    }

    private Mono<Void> updateDeleteStatusInBatches(List<Long> ids, boolean deleted) {
        List<List<Long>> batches = partition(ids, BATCH_SIZE);
        return Flux.fromIterable(batches)
                .flatMap(batch -> r2dbcEntityTemplate.update(
                        Query.query(Criteria.where("id").in(batch)),
                        Update.update("delete_status", deleted),
                        SysMenuEntity.class
                ))
                .then();
    }

    private static List<List<Long>> partition(List<Long> list, int size) {
        List<List<Long>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}

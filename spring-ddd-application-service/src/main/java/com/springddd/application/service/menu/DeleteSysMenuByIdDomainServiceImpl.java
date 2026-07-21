package com.springddd.application.service.menu;

import com.springddd.domain.menu.DeleteSysMenuByIdDomainService;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DeleteSysMenuByIdDomainServiceImpl implements DeleteSysMenuByIdDomainService {

    private static final int BATCH_SIZE = 1000;

    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    private final ReactiveRedisCacheHelper reactiveRedisCacheHelper;

    @Override
    public Mono<Void> deleteByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Mono.empty();
        }
        return r2dbcEntityTemplate.select(SysMenuTreeNodeEntity.class)
                .matching(Query.query(Criteria.where("delete_status").is(false)))
                .all()
                .collectList()
                .flatMap(activeMenus -> {
                    List<Long> allIds = collectDescendantIds(activeMenus, ids);
                    return updateDeleteStatusInBatches(allIds, true)
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

    /**
     * Collect the given root ids plus all descendant ids by walking an in-memory
     * parent-id index built from the supplied menu entities.
     */
    static List<Long> collectDescendantIds(Collection<SysMenuTreeNodeEntity> allMenus, Collection<Long> rootIds) {
        Map<Long, List<SysMenuTreeNodeEntity>> parentIndex = allMenus.stream()
                .filter(e -> e.getParentId() != null)
                .collect(Collectors.groupingBy(SysMenuTreeNodeEntity::getParentId));
        java.util.Set<Long> result = new java.util.LinkedHashSet<>(rootIds);
        ArrayDeque<Long> queue = new ArrayDeque<>(rootIds);
        while (!queue.isEmpty()) {
            Long parentId = queue.poll();
            List<SysMenuTreeNodeEntity> children = parentIndex.get(parentId);
            if (children == null) {
                continue;
            }
            for (SysMenuTreeNodeEntity child : children) {
                if (result.add(child.getId())) {
                    queue.offer(child.getId());
                }
            }
        }
        return result.stream().toList();
    }

    private static List<List<Long>> partition(List<Long> list, int size) {
        List<List<Long>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}

package com.springddd.application.service.common;

import com.springddd.domain.auth.AuthUser;
import com.springddd.domain.auth.ReactiveSecurityUtils;
import com.springddd.domain.role.DataScope;
import com.springddd.infrastructure.persistence.entity.SysDeptEntity;
import com.springddd.infrastructure.persistence.entity.SysPostEntity;
import com.springddd.infrastructure.persistence.entity.SysRoleEntity;
import com.springddd.infrastructure.persistence.entity.SysRowPermissionEntity;
import com.springddd.infrastructure.persistence.entity.SysUserEntity;
import com.springddd.infrastructure.persistence.entity.SysUserPostEntity;
import com.springddd.infrastructure.persistence.r2dbc.SysRoleRepository;
import com.springddd.infrastructure.persistence.r2dbc.SysRowPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DataScopeQueryFilter {

    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final SysRoleRepository sysRoleRepository;
    private final SysRowPermissionRepository sysRowPermissionRepository;

    public Mono<DataScopeResult> apply(Long menuId) {
        if (menuId == null) {
            return Mono.just(DataScopeResult.all());
        }
        return ReactiveSecurityUtils.getCurrentUser()
                .flatMap(authUser -> resolveDataScopeResult(menuId, authUser));
    }

    private Mono<DataScopeResult> resolveDataScopeResult(Long menuId, AuthUser authUser) {
        List<String> roleCodes = authUser.getRoles();
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Mono.just(new DataScopeResult(Set.of(authUser.getUsername())));
        }
        return Flux.fromIterable(roleCodes)
                .flatMap(sysRoleRepository::findByRoleCodeAndDeleteStatusFalse)
                .collectList()
                .flatMap(roles -> {
                    if (roles.isEmpty()) {
                        return Mono.just(new DataScopeResult(Set.of(authUser.getUsername())));
                    }
                    return Flux.fromIterable(roles)
                            .flatMap(role -> resolveEffectiveScope(menuId, role))
                            .collectList()
                            .flatMap(scopes -> {
                                Integer minScope = scopes.stream()
                                        .filter(Objects::nonNull)
                                        .min(Integer::compareTo)
                                        .orElse(null);
                                if (minScope == null) {
                                    // No restriction configured on any role (e.g. admin) -> unrestricted.
                                    return Mono.just(DataScopeResult.all());
                                }
                                DataScope scope = DataScope.of(minScope);
                                if (scope == null) {
                                    // Unknown scope value: fail closed to personal data only.
                                    return Mono.just(new DataScopeResult(Set.of(authUser.getUsername())));
                                }
                                if (scope == DataScope.ALL) {
                                    return Mono.just(DataScopeResult.all());
                                }
                                return resolveByScope(scope, authUser);
                            });
                });
    }

    /**
     * Effective scope of one role on a menu: the widest (minimum value) scope among the
     * role's per-menu rules, or the role's default dataScope when no rule exists.
     */
    private Mono<Integer> resolveEffectiveScope(Long menuId, SysRoleEntity role) {
        return sysRowPermissionRepository
                .findByRoleIdAndMenuIdAndDeleteStatusFalse(role.getId(), menuId)
                .map(SysRowPermissionEntity::getScopeType)
                .filter(Objects::nonNull)
                .collectList()
                .flatMap(configScopes -> configScopes.isEmpty()
                        ? Mono.justOrEmpty(role.getDataScope())
                        : Mono.just(Collections.min(configScopes)));
    }

    private Mono<DataScopeResult> resolveByScope(DataScope scope, AuthUser authUser) {
        return switch (scope) {
            case ALL -> Mono.just(DataScopeResult.all());
            case PERSONAL -> Mono.just(new DataScopeResult(Set.of(authUser.getUsername())));
            case DEPT_ONLY -> resolveDeptScope(false, authUser);
            case DEPT_AND_CHILDREN -> resolveDeptScope(true, authUser);
            case POST -> resolvePostScope(authUser);
        };
    }

    private Mono<DataScopeResult> resolveDeptScope(boolean includeChildren, AuthUser authUser) {
        return findCurrentUser(authUser.getUsername())
                .flatMap(currentUser -> {
                    Long deptId = currentUser.getDeptId();
                    if (deptId == null) {
                        return Mono.just(new DataScopeResult(Set.of(authUser.getUsername())));
                    }
                    Mono<Set<Long>> deptIdsMono = includeChildren
                            ? findAllDepts().map(allDepts -> {
                                Set<Long> ids = new HashSet<>();
                                ids.add(deptId);
                                ids.addAll(findChildrenDeptIds(deptId, allDepts));
                                return ids;
                            })
                            : Mono.just(Set.of(deptId));
                    return deptIdsMono
                            .flatMap(this::findUsernamesByDeptIds)
                            .map(usernames -> usernames.isEmpty()
                                    ? new DataScopeResult(Set.of(authUser.getUsername()))
                                    : new DataScopeResult(usernames));
                })
                .defaultIfEmpty(new DataScopeResult(Set.of(authUser.getUsername())));
    }

    private Mono<DataScopeResult> resolvePostScope(AuthUser authUser) {
        return findCurrentUser(authUser.getUsername())
                .flatMap(currentUser -> findPostIdsOfUser(currentUser.getId())
                        .flatMap(postIds -> {
                            if (postIds.isEmpty()) {
                                return Mono.just(new DataScopeResult(Set.of(authUser.getUsername())));
                            }
                            return expandWithChildPosts(postIds)
                                    .flatMap(this::findUserIdsByPostIds)
                                    .flatMap(this::findUsernamesByIds)
                                    .map(usernames -> usernames.isEmpty()
                                            ? new DataScopeResult(Set.of(authUser.getUsername()))
                                            : new DataScopeResult(usernames));
                        }))
                .defaultIfEmpty(new DataScopeResult(Set.of(authUser.getUsername())));
    }

    private Mono<SysUserEntity> findCurrentUser(String username) {
        return r2dbcEntityTemplate.select(SysUserEntity.class)
                .matching(Query.query(Criteria.where("username").is(username).and("deleteStatus").is(false)))
                .first();
    }

    private Mono<Set<Long>> findPostIdsOfUser(Long userId) {
        return r2dbcEntityTemplate.select(SysUserPostEntity.class)
                .matching(Query.query(Criteria.where("userId").is(userId).and("deleteStatus").is(false)))
                .all()
                .map(SysUserPostEntity::getPostId)
                .collect(Collectors.toSet());
    }

    private Mono<Set<Long>> expandWithChildPosts(Set<Long> postIds) {
        return findAllPosts().map(allPosts -> {
            Set<Long> targetPostIds = new HashSet<>(postIds);
            for (Long postId : postIds) {
                targetPostIds.addAll(findChildrenPostIds(postId, allPosts));
            }
            return targetPostIds;
        });
    }

    private Mono<Set<Long>> findUserIdsByPostIds(Set<Long> postIds) {
        if (postIds.isEmpty()) {
            return Mono.just(Collections.emptySet());
        }
        return Flux.fromIterable(postIds)
                .flatMap(postId -> r2dbcEntityTemplate.select(SysUserPostEntity.class)
                        .matching(Query.query(Criteria.where("postId").is(postId).and("deleteStatus").is(false)))
                        .all())
                .map(SysUserPostEntity::getUserId)
                .collect(Collectors.toSet());
    }

    private Mono<Set<String>> findUsernamesByIds(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Mono.just(Collections.emptySet());
        }
        return Flux.fromIterable(userIds)
                .flatMap(userId -> r2dbcEntityTemplate.select(SysUserEntity.class)
                        .matching(Query.query(Criteria.where("id").is(userId).and("deleteStatus").is(false)))
                        .all())
                .map(SysUserEntity::getUsername)
                .collect(Collectors.toSet());
    }

    private Mono<Set<String>> findUsernamesByDeptIds(Set<Long> deptIds) {
        if (deptIds.isEmpty()) {
            return Mono.just(Collections.emptySet());
        }
        return Flux.fromIterable(deptIds)
                .flatMap(deptId -> r2dbcEntityTemplate.select(SysUserEntity.class)
                        .matching(Query.query(Criteria.where("deptId").is(deptId).and("deleteStatus").is(false)))
                        .all())
                .map(SysUserEntity::getUsername)
                .collect(Collectors.toSet());
    }

    private Mono<List<SysDeptEntity>> findAllDepts() {
        return r2dbcEntityTemplate.select(SysDeptEntity.class)
                .matching(Query.query(Criteria.where("deleteStatus").is(false)))
                .all()
                .collectList();
    }

    private Mono<List<SysPostEntity>> findAllPosts() {
        return r2dbcEntityTemplate.select(SysPostEntity.class)
                .matching(Query.query(Criteria.where("deleteStatus").is(false)))
                .all()
                .collectList();
    }

    private Set<Long> findChildrenDeptIds(Long parentId, List<SysDeptEntity> allDepts) {
        Set<Long> result = new HashSet<>();
        for (SysDeptEntity dept : allDepts) {
            if (parentId.equals(dept.getParentId())) {
                result.add(dept.getId());
                result.addAll(findChildrenDeptIds(dept.getId(), allDepts));
            }
        }
        return result;
    }

    private Set<Long> findChildrenPostIds(Long parentId, List<SysPostEntity> allPosts) {
        Set<Long> result = new HashSet<>();
        for (SysPostEntity post : allPosts) {
            if (parentId.equals(post.getParentId())) {
                result.add(post.getId());
                result.addAll(findChildrenPostIds(post.getId(), allPosts));
            }
        }
        return result;
    }

    /**
     * Work around asyncer-r2dbc-mysql incorrectly using executeMany for Criteria#in(Collection).
     * Converts a set of usernames into an OR-chained Criteria (field = u1 OR field = u2 ...).
     * An empty set returns field IS NULL to match no records.
     */
    public static Criteria createByInCriteria(String field, Set<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return Criteria.where(field).is(null);
        }
        Iterator<String> iterator = usernames.iterator();
        Criteria criteria = Criteria.where(field).is(iterator.next());
        while (iterator.hasNext()) {
            criteria = criteria.or(field).is(iterator.next());
        }
        return criteria;
    }
}

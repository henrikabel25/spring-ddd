package com.springddd.application.service.menu;

import com.springddd.application.service.auth.jwt.JwtSecret;
import com.springddd.application.service.menu.dto.SysMenuQuery;
import com.springddd.application.service.menu.dto.SysMenuView;
import com.springddd.application.service.menu.dto.SysMenuViewMapStruct;
import com.springddd.application.service.role.SysRoleQueryService;
import com.springddd.domain.auth.SecurityUtils;
import com.springddd.domain.role.RoleCode;
import com.springddd.domain.util.PageResponse;
import com.springddd.domain.util.ReactiveTreeUtils;
import com.springddd.infrastructure.cache.util.ReactiveRedisCacheHelper;
import com.springddd.infrastructure.persistence.entity.SysMenuEntity;
import com.springddd.infrastructure.persistence.r2dbc.SysMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysMenuQueryService {

    private final SysMenuRepository sysMenuRepository;

    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    private final SysMenuViewMapStruct sysMenuViewMapStruct;

    private final ReactiveRedisCacheHelper reactiveRedisCacheHelper;

    private final JwtSecret jwtSecret;

    private final SysRoleQueryService sysRoleQueryService;

    public Mono<PageResponse<SysMenuView>> index(SysMenuQuery query) {
        Criteria criteria = Criteria.where("delete_status").is(false);
        Query qry = Query.query(criteria)
                .limit(Integer.MAX_VALUE)
                .offset(0L);
        Mono<List<SysMenuView>> list = r2dbcEntityTemplate.select(SysMenuEntity.class).matching(qry).all().collectList().map(sysMenuViewMapStruct::toViewList);
        Mono<Long> count = r2dbcEntityTemplate.count(Query.query(criteria), SysMenuEntity.class);
        return Mono.zip(list, count).map(tuple -> new PageResponse<>(tuple.getT1(), tuple.getT2(), 11, 1));
    }

    public Mono<SysMenuView> queryByMenuId(Long menuId) {
        return sysMenuRepository.findById(menuId).map(sysMenuViewMapStruct::toView);
    }

    public Mono<SysMenuView> queryByMenuComponent(String component) {
        Criteria criteria = Criteria.where("component").is(component);
        Query qry = Query.query(criteria);
        return r2dbcEntityTemplate.selectOne(qry, SysMenuEntity.class).map(sysMenuViewMapStruct::toView);
    }

    public Mono<List<SysMenuView>> queryByPermissions() {
        return Flux.fromIterable(SecurityUtils.getPermissions())
                .flatMap(p ->
                        r2dbcEntityTemplate.selectOne(
                                Query.query(Criteria.where("permission").is(p.value())),
                                SysMenuEntity.class
                        ).map(sysMenuViewMapStruct::toView)
                )
                .collectList()
                .flatMap(this::loadParentsAndBuildTree)
                .flatMap(menus -> {
                    Mono<Void> withPermissionsTreeCache = cacheMenuWithPermissionsTree(menus);

                    List<SysMenuView> withoutPermissionsTree = extractMenuWithoutPermissionsTree(menus);
                    Mono<Void> withoutPermissionsTreeCache = cacheMenuWithoutPermissionsTree(withoutPermissionsTree);

                    return Mono.when(withPermissionsTreeCache, withoutPermissionsTreeCache).thenReturn(withoutPermissionsTree);
                });
    }

    private Mono<List<SysMenuView>> loadParentsAndBuildTree(List<SysMenuView> menus) {
        if (CollectionUtils.isEmpty(menus)) {
            return Mono.empty();
        }
        Map<Long, SysMenuView> menuMap = new ConcurrentHashMap<>();
        menus.forEach(menu -> menuMap.put(menu.getId(), menu));

        return Flux.fromIterable(menus)
                .flatMap(menu -> loadParentChain(menu.getParentId(), menuMap))
                .then(Mono.fromCallable(() -> new ArrayList<>(menuMap.values())))
                .flatMap(flatList -> ReactiveTreeUtils.buildTree(
                        flatList,
                        SysMenuView::getId,
                        SysMenuView::getParentId,
                        SysMenuView::setChildren,
                        menu -> menu.getParentId() == null || menu.getParentId() == 0,
                        null,
                        null,
                        30
                ));
    }

    private Mono<Void> loadParentChain(Long parentId, Map<Long, SysMenuView> menuMap) {
        if (parentId == null || parentId == 0 || menuMap.containsKey(parentId)) {
            return Mono.empty();
        }

        return r2dbcEntityTemplate.selectOne(
                        Query.query(Criteria.where("id").is(parentId)),
                        SysMenuEntity.class
                )
                .map(sysMenuViewMapStruct::toView)
                .flatMap(parent -> {
                    menuMap.put(parent.getId(), parent);
                    return loadParentChain(parent.getParentId(), menuMap);
                });
    }

    private Mono<Void> cacheMenuWithPermissionsTree(List<SysMenuView> menus) {
        return reactiveRedisCacheHelper.setCache("user:" + SecurityUtils.getUserId() + ":menuWithPermissions", menus, Duration.ofDays(jwtSecret.getTtl())).then();
    }

    public Mono<List<SysMenuView>> getMenuTreeWithoutPermission() {
        return reactiveRedisCacheHelper
                .getCache("user:" + SecurityUtils.getUserId().toString() + ":menuWithoutPermissions", List.class)
                .map(list -> (List<SysMenuView>) list).switchIfEmpty(Mono.error(new RuntimeException("No menus found")));
    }

    public Mono<List<SysMenuView>> getMenuTreeWithPermission() {
        List<RoleCode> codes = SecurityUtils.getRoles();

        if (CollectionUtils.isEmpty(codes)) {
            return Mono.empty();
        }

        return Flux.fromIterable(codes)
                .flatMap(code -> sysRoleQueryService.getByCode(code.value()))
                .filter(Objects::nonNull)
                .filter(role -> Boolean.TRUE.equals(role.getOwnerStatus()))
                .hasElements()
                .flatMap(hasOwner -> {
                    if (hasOwner) {
                        return r2dbcEntityTemplate.select(SysMenuEntity.class)
                                .matching(Query.empty())
                                .all()
                                .collectList()
                                .map(sysMenuViewMapStruct::toViewList)
                                .flatMap(this::loadParentsAndBuildTree);
                    } else {
                        return reactiveRedisCacheHelper
                                .getCache("user:" + SecurityUtils.getUserId().toString() + ":menuWithPermissions", List.class)
                                .map(list -> (List<SysMenuView>) list)
                                .switchIfEmpty(Mono.error(new RuntimeException("No menus found")));
                    }
                });
    }

    private List<SysMenuView> extractMenuWithoutPermissionsTree(List<SysMenuView> menus) {
        return menus.stream()
                .map(this::filterOutInvalidMenusRecursively)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private SysMenuView filterOutInvalidMenusRecursively(SysMenuView menu) {
        if (menu.getMenuType() == 3) {
            return null;
        }

        List<SysMenuView> validChildren = Optional.ofNullable(menu.getChildren())
                .orElse(Collections.emptyList())
                .stream()
                .map(this::filterOutInvalidMenusRecursively)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        SysMenuView copy = copyMenuWithoutChildren(menu);
        copy.setChildren(validChildren);
        return copy;
    }

    private SysMenuView copyMenuWithoutChildren(SysMenuView menu) {
        SysMenuView copy = new SysMenuView();
        copy.setMeta(menu.getMeta());
        copy.setId(menu.getId());
        copy.setParentId(menu.getParentId());
        copy.setMenuType(menu.getMenuType());
        copy.setPath(menu.getPath());
        copy.setName(menu.getName());
        copy.setComponent(menu.getComponent());
        copy.setPermission(menu.getPermission());
        copy.setMenuType(menu.getMenuType());
        copy.setVisible(menu.getVisible());
        copy.setEmbedded(menu.getEmbedded());
        copy.setMenuStatus(menu.getMenuStatus());
        return copy;
    }

    private Mono<Void> cacheMenuWithoutPermissionsTree(List<SysMenuView> menus) {
        return reactiveRedisCacheHelper.setCache("user:" + SecurityUtils.getUserId() + ":menuWithoutPermissions", menus, Duration.ofDays(jwtSecret.getTtl())).then();
    }
}

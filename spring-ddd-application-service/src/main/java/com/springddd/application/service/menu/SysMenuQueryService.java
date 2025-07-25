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
        Criteria criteria = Criteria.where(SysMenuQuery.Fields.deleteStatus).is(false);
        Query qry = Query.query(criteria);
        Mono<List<SysMenuView>> list = r2dbcEntityTemplate.select(SysMenuEntity.class).matching(qry).all().collectList().map(sysMenuViewMapStruct::toViewList);
        Mono<Long> count = r2dbcEntityTemplate.count(Query.query(criteria), SysMenuEntity.class);
        return Mono.zip(list, count).map(tuple -> new PageResponse<>(tuple.getT1(), tuple.getT2(), 0, 0));
    }

    public Mono<PageResponse<SysMenuView>> recycle(SysMenuQuery query) {
        Criteria criteria = Criteria.where(SysMenuQuery.Fields.deleteStatus).is(true);
        Query qry = Query.query(criteria);
        Mono<List<SysMenuView>> list = r2dbcEntityTemplate.select(SysMenuEntity.class).matching(qry).all().collectList().map(sysMenuViewMapStruct::toViewList);
        Mono<Long> count = r2dbcEntityTemplate.count(Query.query(criteria), SysMenuEntity.class);
        return Mono.zip(list, count).map(tuple -> new PageResponse<>(tuple.getT1(), tuple.getT2(), 0, 0));
    }

    public Mono<SysMenuView> queryByMenuId(Long menuId) {
        return sysMenuRepository.findById(menuId).filter(menu -> !menu.getDeleteStatus()).map(sysMenuViewMapStruct::toView);
    }

    public Mono<SysMenuView> queryByMenuComponent(String component) {
        Criteria criteria = Criteria.where(SysMenuQuery.Fields.component).is(component);
        Query qry = Query.query(criteria);
        return r2dbcEntityTemplate.selectOne(qry, SysMenuEntity.class).map(sysMenuViewMapStruct::toView);
    }

    public Mono<List<SysMenuView>> queryByPermissions() {
        return Flux.fromIterable(SecurityUtils.getMenuIds())
                .flatMap(mId ->
                        r2dbcEntityTemplate.selectOne(
                                Query.query(Criteria
                                        .where(SysMenuQuery.Fields.id).is(mId)
                                        .and(SysMenuQuery.Fields.deleteStatus).is(false)),
                                SysMenuEntity.class
                        ).map(sysMenuViewMapStruct::toView)
                )
                .collectList()
                .flatMap(menus -> ReactiveTreeUtils.loadParentsAndBuildTree(
                        menus,
                        SysMenuView::getId,
                        SysMenuView::getParentId,
                        parentId -> r2dbcEntityTemplate.selectOne(
                                Query.query(Criteria
                                        .where(SysMenuQuery.Fields.id).is(parentId)
                                        .and(SysMenuQuery.Fields.deleteStatus).is(false)),
                                SysMenuEntity.class
                        ).map(sysMenuViewMapStruct::toView),
                        SysMenuView::setChildren,
                        menu -> menu.getParentId() == null || menu.getParentId() == 0,
                        Comparator.comparing(o -> o.getMeta().getOrder()),
                        menu -> !menu.getDeleteStatus(),
                        30,
                        menu -> !menu.getDeleteStatus()
                ))
                .flatMap(menus -> {
                    Mono<Void> withPermissionsTreeCache = cacheMenuWithPermissionsTree(menus);

                    List<SysMenuView> withoutPermissionsTree = extractMenuWithoutPermissionsTree(menus);
                    Mono<Void> withoutPermissionsTreeCache = cacheMenuWithoutPermissionsTree(withoutPermissionsTree);

                    return Mono.when(withPermissionsTreeCache, withoutPermissionsTreeCache).thenReturn(withoutPermissionsTree);
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
                                .matching(Query.query(Criteria.where(SysMenuQuery.Fields.deleteStatus).is(false)))
                                .all()
                                .collectList()
                                .map(sysMenuViewMapStruct::toViewList)
                                .flatMap(menus -> ReactiveTreeUtils.loadParentsAndBuildTree(
                                        menus,
                                        SysMenuView::getId,
                                        SysMenuView::getParentId,
                                        parentId -> r2dbcEntityTemplate.selectOne(
                                                Query.query(
                                                        Criteria.where(SysMenuQuery.Fields.id).is(parentId)
                                                        .and(SysMenuQuery.Fields.deleteStatus).is(false)),
                                                SysMenuEntity.class
                                        ).map(sysMenuViewMapStruct::toView),
                                        SysMenuView::setChildren,
                                        menu -> menu.getParentId() == null || menu.getParentId() == 0,
                                        Comparator.comparing(o -> o.getMeta().getOrder()),
                                        menu -> !menu.getDeleteStatus(),
                                        30,
                                        m -> !m.getDeleteStatus()
                                ));
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

    public Mono<List<SysMenuView>> queryAllMenu() {
        return sysMenuRepository.findAll().collectList().map(sysMenuViewMapStruct::toViewList);
    }
}

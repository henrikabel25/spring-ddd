package com.springddd.application.service.menu;

import com.springddd.application.service.menu.dto.SysMenuCommand;
import com.springddd.domain.menu.*;
import com.springddd.infrastructure.cache.util.ReactiveRedisCacheHelper;
import com.springddd.infrastructure.persistence.entity.SysMenuEntity;
import com.springddd.infrastructure.persistence.r2dbc.SysMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SysMenuCommandService {

    private final SysMenuDomainFactory sysMenuDomainFactory;

    private final SysMenuDomainRepository sysMenuDomainRepository;

    private final SysMenuRepository sysMenuRepository;

    private final WipeSysMenuByIdsDomainService wipeSysMenuByIdsDomainService;

    private final List<SysMenuDomainStrategy> strategies;

    private final DeleteSysMenuByIdDomainService deleteSysMenuByIdDomainService;

    private final RestoreSysMenuByIdDomainService restoreSysMenuByIdDomainService;

    private final ReactiveRedisCacheHelper reactiveRedisCacheHelper;

    @Transactional(rollbackFor = Exception.class)
    public Mono<Long> create(SysMenuCommand command) {
        return computeParentInfo(command.getParentId())
                .flatMap(parentInfo -> {
                    Catalog catalog = new Catalog(command.getRedirect());
                    Menu menu = new Menu(command.getPath(), command.getComponent(), command.getAffixTab(), command.getNoBasicLayout(), command.getEmbedded());
                    Button button = (command.getMenuType() != null && command.getMenuType() == 3)
                            ? new Button(command.getPermission(), command.getApi())
                            : null;
                    MenuExtendInfo menuExtendInfo = new MenuExtendInfo(
                            command.getOrder(),
                            command.getTitle(),
                            command.getIcon(),
                            command.getMenuType(),
                            command.getVisible(),
                            command.getMenuStatus());

                    SysMenuDomain sysMenuDomain = sysMenuDomainFactory.create(
                            new MenuId(command.getParentId()), command.getName(), catalog, menu, button, menuExtendInfo, command.getDeptId());
                    sysMenuDomain.setDepth(parentInfo.depth());
                    sysMenuDomain.create();

                    return sysMenuDomainRepository.save(sysMenuDomain)
                            .map(id -> {
                                String treePath = buildTreePath(parentInfo.treePath(), id);
                                sysMenuDomain.setMenuId(new MenuId(id));
                                sysMenuDomain.setTreePath(treePath);
                                return sysMenuDomain;
                            })
                            .flatMap(sysMenuDomainRepository::save)
                            .doFinally(signalType -> reactiveRedisCacheHelper
                                    .deleteCache("user:*:menuWithPermissions")
                                    .then(reactiveRedisCacheHelper.deleteCache("user:*:menuWithoutPermissions"))
                                    .then(reactiveRedisCacheHelper.deleteCache("auth:user:detail:*").onErrorResume(e -> Mono.empty()))
                                    .subscribe());
                });
    }

    @Transactional(rollbackFor = Exception.class)
    public Mono<Void> update(SysMenuCommand command) {
        return sysMenuDomainRepository.load(new MenuId(command.getId())).flatMap(domain -> {
            Long oldParentId = domain.getParentId() != null ? domain.getParentId().value() : null;
            Long newParentId = command.getParentId();
            boolean parentChanged = !java.util.Objects.equals(oldParentId, newParentId);

            return computeParentInfo(newParentId)
                    .flatMap(newParentInfo -> {
                        if (parentChanged) {
                            // Prevent cycles: new parent must not be a descendant of current node.
                            return validateNoCycle(command.getId(), newParentId)
                                    .then(Mono.defer(() -> {
                                        Integer oldDepth = domain.getDepth();
                                        String oldTreePath = domain.getTreePath();
                                        Integer newDepth = newParentInfo.depth();
                                        String newTreePath = buildTreePath(newParentInfo.treePath(), command.getId());

                                        Catalog catalog = new Catalog(command.getRedirect());
                                        Menu menu = new Menu(command.getPath(), command.getComponent(), command.getAffixTab(), command.getNoBasicLayout(), command.getEmbedded());
                                        Button button = (command.getMenuType() != null && command.getMenuType() == 3)
                                                ? new Button(command.getPermission(), command.getApi())
                                                : domain.getButton();
                                        MenuExtendInfo menuExtendInfo = new MenuExtendInfo(
                                                command.getOrder(),
                                                command.getTitle(),
                                                command.getIcon(),
                                                command.getMenuType(),
                                                command.getVisible(),
                                                command.getMenuStatus());

                                        SysMenuDomain dummy = sysMenuDomainFactory.create(
                                                new MenuId(command.getParentId()), command.getName(), catalog, menu, button, menuExtendInfo, command.getDeptId());

                                        domain.update(new MenuId(command.getParentId()), dummy.getName(), dummy.getCatalog(), dummy.getMenu(), dummy.getButton(), dummy.getMenuExtendInfo(), command.getDeptId());
                                        domain.setDepth(newDepth);
                                        domain.setTreePath(newTreePath);

                                        return sysMenuDomainRepository.save(domain)
                                                .flatMap(savedId -> updateDescendantsIfNeeded(command.getId(), oldDepth, oldTreePath, newDepth, newTreePath))
                                                .doFinally(signalType -> reactiveRedisCacheHelper
                                                        .deleteCache("user:*:menuWithPermissions")
                                                        .then(reactiveRedisCacheHelper.deleteCache("user:*:menuWithoutPermissions"))
                                                        .then(reactiveRedisCacheHelper.deleteCache("auth:user:detail:*").onErrorResume(e -> Mono.empty()))
                                                        .subscribe());
                                    }));
                        } else {
                            Catalog catalog = new Catalog(command.getRedirect());
                            Menu menu = new Menu(command.getPath(), command.getComponent(), command.getAffixTab(), command.getNoBasicLayout(), command.getEmbedded());
                            Button button = (command.getMenuType() != null && command.getMenuType() == 3)
                                    ? new Button(command.getPermission(), command.getApi())
                                    : domain.getButton();
                            MenuExtendInfo menuExtendInfo = new MenuExtendInfo(
                                    command.getOrder(),
                                    command.getTitle(),
                                    command.getIcon(),
                                    command.getMenuType(),
                                    command.getVisible(),
                                    command.getMenuStatus());

                            SysMenuDomain dummy = sysMenuDomainFactory.create(
                                    new MenuId(command.getParentId()), command.getName(), catalog, menu, button, menuExtendInfo, command.getDeptId());

                            domain.update(new MenuId(command.getParentId()), dummy.getName(), dummy.getCatalog(), dummy.getMenu(), dummy.getButton(), dummy.getMenuExtendInfo(), command.getDeptId());
                            return sysMenuDomainRepository.save(domain).then()
                                    .doFinally(signalType -> reactiveRedisCacheHelper
                                            .deleteCache("user:*:menuWithPermissions")
                                            .then(reactiveRedisCacheHelper.deleteCache("user:*:menuWithoutPermissions"))
                                            .then(reactiveRedisCacheHelper.deleteCache("auth:user:detail:*").onErrorResume(e -> Mono.empty()))
                                            .subscribe());
                        }
                    });
        }).then();
    }

    public Mono<Void> delete(List<Long> ids) {
        return deleteSysMenuByIdDomainService.deleteByIds(ids);
    }

    public Mono<Void> wipe(List<Long> ids) {
        return wipeSysMenuByIdsDomainService.deleteByIds(ids);
    }

    public Mono<Void> restore(List<Long> ids) {
        return restoreSysMenuByIdDomainService.restoreByIds(ids);
    }

    private Mono<ParentInfo> computeParentInfo(Long parentId) {
        if (parentId == null || parentId == 0) {
            return Mono.just(new ParentInfo(0, "/"));
        }
        return sysMenuDomainRepository.load(new MenuId(parentId))
                .map(parent -> new ParentInfo(
                        parent.getDepth() != null ? parent.getDepth() + 1 : 1,
                        parent.getTreePath()))
                .switchIfEmpty(Mono.just(new ParentInfo(0, "/")));
    }

    private String buildTreePath(String parentTreePath, Long id) {
        if (parentTreePath == null) {
            return null;
        }
        String base = parentTreePath.endsWith("/") ? parentTreePath : parentTreePath + "/";
        String candidate = base + id + "/";
        // Do not materialize paths that exceed the column length (500).
        return candidate.length() <= 500 ? candidate : null;
    }

    private Mono<Void> validateNoCycle(Long currentId, Long newParentId) {
        if (newParentId == null || newParentId == 0 || currentId == null) {
            return Mono.empty();
        }
        return collectAncestorIds(newParentId)
                .any(ancestorId -> ancestorId.equals(currentId))
                .flatMap(hasCycle -> {
                    if (Boolean.TRUE.equals(hasCycle)) {
                        return Mono.error(new IllegalArgumentException("Cannot move a node under its own descendant"));
                    }
                    return Mono.empty();
                });
    }

    private Flux<Long> collectAncestorIds(Long startId) {
        return Flux.defer(() -> Flux.create(sink -> {
            Long currentId = startId;
            Set<Long> visited = new HashSet<>();
            while (currentId != null && !visited.contains(currentId)) {
                visited.add(currentId);
                sink.next(currentId);
                SysMenuEntity parent = sysMenuRepository.findById(currentId).block();
                if (parent == null) {
                    break;
                }
                currentId = parent.getParentId();
            }
            sink.complete();
        }));
    }

    private Mono<Void> updateDescendantsIfNeeded(Long menuId, Integer oldDepth, String oldTreePath,
                                                   Integer newDepth, String newTreePath) {
        if (menuId == null || oldDepth == null || newDepth == null) {
            return Mono.empty();
        }
        if (oldDepth.equals(newDepth) && java.util.Objects.equals(oldTreePath, newTreePath)) {
            return Mono.empty();
        }
        int depthDelta = newDepth - oldDepth;

        return sysMenuRepository.findByParentIdAndDeleteStatus(menuId, false)
                .map(SysMenuEntity::getId)
                .collectList()
                .flatMap(children -> updateSubtreeIteratively(children, oldDepth, oldTreePath, newDepth, newTreePath, depthDelta));
    }

    /**
     * Iteratively update depth/tree_path for a subtree using an explicit stack.
     * Avoids Reactor operator recursion which causes StackOverflowError at million-level depth.
     */
    private Mono<Void> updateSubtreeIteratively(List<Long> initialNodeIds,
                                                   Integer ancestorOldDepth, String ancestorOldTreePath,
                                                   Integer ancestorNewDepth, String ancestorNewTreePath,
                                                   int depthDelta) {
        if (initialNodeIds == null || initialNodeIds.isEmpty()) {
            return Mono.empty();
        }

        Deque<NodeUpdateFrame> stack = new ArrayDeque<>();
        for (Long nodeId : initialNodeIds) {
            stack.push(new NodeUpdateFrame(nodeId, ancestorOldDepth, ancestorOldTreePath, ancestorNewDepth, ancestorNewTreePath));
        }

        return Mono.defer(() -> processUpdateFrames(stack, depthDelta)).then();
    }

    private Mono<Void> processUpdateFrames(Deque<NodeUpdateFrame> stack, int depthDelta) {
        if (stack.isEmpty()) {
            return Mono.empty();
        }
        NodeUpdateFrame frame = stack.pop();
        return sysMenuRepository.findById(frame.nodeId())
                .flatMap(entity -> {
                    Integer oldDepth = entity.getDepth();
                    String oldTreePath = entity.getTreePath();
                    Integer newDepth = oldDepth != null ? oldDepth + depthDelta : null;
                    String newTreePath = computeNewTreePath(oldTreePath, frame.ancestorOldTreePath(), frame.ancestorNewTreePath());
                    entity.setDepth(newDepth);
                    entity.setTreePath(newTreePath);
                    return sysMenuRepository.save(entity)
                            .then(sysMenuRepository.findByParentIdAndDeleteStatus(frame.nodeId(), false)
                                    .map(SysMenuEntity::getId)
                                    .collectList()
                                    .doOnNext(children -> {
                                        for (Long childId : children) {
                                            stack.push(new NodeUpdateFrame(childId, oldDepth, oldTreePath, newDepth, newTreePath));
                                        }
                                    })
                                    .then(processUpdateFrames(stack, depthDelta)));
                })
                .switchIfEmpty(processUpdateFrames(stack, depthDelta));
    }

    private String computeNewTreePath(String oldTreePath, String ancestorOldTreePath, String ancestorNewTreePath) {
        if (oldTreePath == null) {
            return null;
        }
        if (ancestorOldTreePath == null || ancestorNewTreePath == null) {
            return oldTreePath;
        }
        if (!oldTreePath.startsWith(ancestorOldTreePath)) {
            return oldTreePath;
        }
        String suffix = oldTreePath.substring(ancestorOldTreePath.length());
        String prefix = ancestorNewTreePath.endsWith("/") ? ancestorNewTreePath : ancestorNewTreePath + "/";
        String candidate = prefix + suffix;
        return candidate.length() <= 500 ? candidate : null;
    }

    private record NodeUpdateFrame(Long nodeId, Integer ancestorOldDepth, String ancestorOldTreePath,
                                   Integer ancestorNewDepth, String ancestorNewTreePath) {
    }

    private record ParentInfo(Integer depth, String treePath) {
    }
}

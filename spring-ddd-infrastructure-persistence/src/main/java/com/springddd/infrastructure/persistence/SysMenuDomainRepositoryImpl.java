package com.springddd.infrastructure.persistence;

import com.springddd.domain.menu.*;
import com.springddd.infrastructure.persistence.entity.SysMenuEntity;
import com.springddd.infrastructure.persistence.r2dbc.SysMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SysMenuDomainRepositoryImpl implements SysMenuDomainRepository {

    private final SysMenuRepository sysMenuRepository;

    @Override
    public Mono<SysMenuDomain> load(MenuId aggregateRootId) {
        return sysMenuRepository.findById(aggregateRootId.value()).flatMap(e -> {
            SysMenuDomain sysMenuDomain = new SysMenuDomain();
            sysMenuDomain.setMenuId(new MenuId(e.getId()));
            sysMenuDomain.setParentId(new MenuId(e.getParentId()));
            sysMenuDomain.setDepth(e.getDepth());
            sysMenuDomain.setTreePath(e.getTreePath());

            Catalog catalog = new Catalog(e.getRedirect());
            sysMenuDomain.setCatalog(catalog);

            sysMenuDomain.setName(e.getName());

            Menu menu = new Menu(e.getPath(), e.getComponent(), e.getAffixTab(), e.getNoBasicLayout(), e.getEmbedded());
            sysMenuDomain.setMenu(menu);

            if (e.getPermission() != null || e.getApi() != null) {
                sysMenuDomain.setButton(new Button(e.getPermission(), e.getApi()));
            }

            MenuExtendInfo menuExtendInfo = new MenuExtendInfo(e.getSortOrder(), e.getTitle(), e.getIcon(), e.getMenuType(), e.getVisible(), e.getMenuStatus());
            sysMenuDomain.setMenuExtendInfo(menuExtendInfo);

            sysMenuDomain.setDeptId(e.getDeptId());
            sysMenuDomain.setDeleteStatus(e.getDeleteStatus());
            sysMenuDomain.setVersion(e.getVersion());
            sysMenuDomain.setCreateBy(e.getCreateBy());
            sysMenuDomain.setCreateTime(e.getCreateTime());
            sysMenuDomain.setUpdateBy(e.getUpdateBy());
            sysMenuDomain.setUpdateTime(e.getUpdateTime());
            return Mono.just(sysMenuDomain);
        });
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Mono<Long> save(SysMenuDomain aggregateRoot) {
        SysMenuEntity entity = new SysMenuEntity();

        entity.setId(Optional.ofNullable(aggregateRoot.getMenuId()).map(MenuId::value).orElse(null));
        entity.setParentId(Optional.ofNullable(aggregateRoot.getParentId()).map(MenuId::value).orElse(null));
        entity.setDepth(aggregateRoot.getDepth());
        entity.setTreePath(aggregateRoot.getTreePath());

        Catalog catalog = aggregateRoot.getCatalog();
        if (!ObjectUtils.isEmpty(catalog)) {
            entity.setRedirect(catalog.menuRedirect());
        }

        entity.setName(aggregateRoot.getName());
        Menu menu = aggregateRoot.getMenu();
        if (menu != null) {
            entity.setPath(menu.menuPath());
            entity.setComponent(menu.component());
            entity.setAffixTab(menu.affixTab());
            entity.setNoBasicLayout(menu.noBasicLayout());
            entity.setEmbedded(menu.embedded());
        }

        Button button = aggregateRoot.getButton();
        if (button != null) {
            entity.setPermission(button.permission());
            entity.setApi(button.api());
        }

        MenuExtendInfo menuExtendInfo = aggregateRoot.getMenuExtendInfo();
        entity.setSortOrder(menuExtendInfo.order());
        entity.setTitle(menuExtendInfo.title());
        entity.setIcon(menuExtendInfo.icon());
        entity.setMenuType(menuExtendInfo.menuType());
        entity.setVisible(menuExtendInfo.visible());
        entity.setMenuStatus(menuExtendInfo.menuStatus());

        entity.setDeptId(aggregateRoot.getDeptId());
        entity.setDeleteStatus(aggregateRoot.getDeleteStatus());
        // Preserve the aggregate version so updates retain the correct optimistic-lock value.
        // Null version indicates a new entity to Spring Data R2DBC.
        entity.setVersion(aggregateRoot.getVersion());
        entity.setCreateBy(Optional.ofNullable(aggregateRoot.getCreateBy()).orElse("system"));
        entity.setCreateTime(Optional.ofNullable(aggregateRoot.getCreateTime()).orElse(LocalDateTime.now()));
        entity.setUpdateBy(Optional.ofNullable(aggregateRoot.getUpdateBy()).orElse("system"));
        entity.setUpdateTime(Optional.ofNullable(aggregateRoot.getUpdateTime()).orElse(LocalDateTime.now()));

        return sysMenuRepository.save(entity)
                .doOnNext(saved -> {
                    aggregateRoot.setMenuId(new MenuId(saved.getId()));
                    aggregateRoot.setVersion(Optional.ofNullable(saved.getVersion()).orElse(0));
                })
                .map(SysMenuEntity::getId);
    }
}

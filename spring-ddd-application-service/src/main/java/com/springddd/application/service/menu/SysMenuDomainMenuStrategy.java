package com.springddd.application.service.menu;

import com.springddd.domain.menu.*;
import org.springframework.stereotype.Component;

@Component
public class SysMenuDomainMenuStrategy implements SysMenuDomainStrategy {

    @Override
    public boolean check(Integer type) {
        return 2 == type;
    }

    @Override
    public SysMenuDomain handle(String name, Catalog catalog, Menu menu, Button button, MenuExtendInfo menuExtendInfo) {
        SysMenuDomain domain = new SysMenuDomain();
        domain.setName(name);

        Menu m = new Menu(menu.menuPath(), menu.component(), menu.affixTab(), menu.noBasicLayout(), menu.embedded());

        domain.setMenu(m);

        domain.setButton(button);

        MenuExtendInfo extendInfo = new MenuExtendInfo(menuExtendInfo.order(), menuExtendInfo.title(), menuExtendInfo.icon(), menuExtendInfo.menuType(), menuExtendInfo.visible(), menuExtendInfo.menuStatus());
        domain.setMenuExtendInfo(extendInfo);
        return domain;
    }
}

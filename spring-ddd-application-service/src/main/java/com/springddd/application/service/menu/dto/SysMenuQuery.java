package com.springddd.application.service.menu.dto;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serializable;

@Data
@FieldNameConstants
public class SysMenuQuery implements Serializable {

    private Long id;

    private Long parentId;

    private String name;

    private String path;

    private String component;

    private String redirect;

    private String permission;

    private Integer order;

    private String title;

    private Boolean affixTab;

    private Boolean noBasicLayout;

    private String icon;

    private Integer menuType;

    private Boolean visible;

    private Boolean embedded;

    private Boolean menuStatus;

    private Long deptId;

    private Boolean deleteStatus;
}

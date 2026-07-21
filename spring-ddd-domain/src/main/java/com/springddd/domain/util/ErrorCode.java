package com.springddd.domain.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    USER_NAME_NULL(1000, "error.user.name.null"),
    USER_PASSWORD_NULL(1001, "error.user.password.null"),
    USER_NOT_FOUND(1002, "error.user.notFound"),

    ROLE_CODE_NULL(1100, "error.role.code.null"),
    ROLE_NAME_NULL(1101, "error.role.name.null"),
    ROLE_DATA_SCOPE_NULL(1102, "error.role.dataScope.null"),
    ROLE_DATA_SCOPE_INVALID(1105, "error.role.dataScope.invalid"),
    ROLE_ID_NULL(1103, "error.role.id.null"),
    ROLE_STATUS_NULL(1104, "error.role.status.null"),

    MENU_NAME_NULL(1200, "error.menu.name.null"),
    MENU_PERMISSION_NULL(1201, "error.menu.permission.null"),
    MENU_TYPE_NULL(1202, "error.menu.type.null"),
    MENU_ICON_NULL(1203, "error.menu.icon.null"),
    MENU_PATH_NULL(1204, "error.menu.path.null"),
    MENU_ID_NULL(1205, "error.menu.id.null"),
    MENU_COMPONENT_NULL(1206, "error.menu.component.null"),
    MENU_PERMISSION_DENIED(1207, "error.menu.permission.denied"),

    DEPT_NAME_NULL(1300, "error.dept.name.null"),
    DEPT_ID_NULL(1301, "error.dept.id.null"),
    DEPT_SORT_ORDER_NULL(1302, "error.dept.sortOrder.null"),
    DEPT_STATUS_NULL(1303, "error.dept.status.null"),

    DICT_NAME_NULL(1400, "error.dict.name.null"),
    DICT_CODE_NULL(1401, "error.dict.code.null"),
    DICT_SORTORDER_NULL(1402, "error.dict.sortOrder.null"),
    DICT_DICTSTATUS_NULL(1403, "error.dict.dictStatus.null"),
    DICT_ID_NULL(1404, "error.dict.id.null"),
    DICT_ITEM_LABEL_NULL(1405, "error.dict.item.label.null"),
    DICT_ITEM_VALUE_NULL(1406, "error.dict.item.value.null"),
    DICT_ITEM_SORTORDER_NULL(1407, "error.dict.item.sortOrder.null"),
    DICT_ITEM_ITEMSTATUS_NULL(1408, "error.dict.item.itemStatus.null"),

    GEN_INFO_PACKAGE_NAME_NULL(1500, "error.gen.info.package.name.null"),
    GEN_INFO_TABLE_NAME_NULL(1501, "error.gen.info.table.name.null"),
    GEN_INFO_CLASS_NAME_NULL(1502, "error.gen.info.class.name.null"),
    GEN_INFO_REQUEST_NAME_NULL(1503, "error.gen.info.request.name.null"),
    GEN_INFO_VALUE_OBJECT_NULL(1504, "error.gen.info.value.object.null"),
    GEN_INFO_COLUMN_KEY_NULL(1505, "error.gen.info.column.key.null"),
    GEN_INFO_COLUMN_NAME_NULL(1506, "error.gen.info.column.name.null"),
    GEN_INFO_COLUMN_TYPE_NULL(1507, "error.gen.info.column.type.null"),
    GEN_INFO_COLUMN_COMMENT_NULL(1508, "error.gen.info.column.comment.null"),
    GEN_INFO_JAVA_ENTITY_NULL(1509, "error.gen.info.java.entity.null"),
    GEN_INFO_JAVA_TYPE_NULL(1510, "error.gen.info.java.type.null"),
    GEN_INFO_DICT_ID_NULL(1511, "error.gen.info.dict.id.null"),
    GEN_INFO_VISIBLE_NULL(1512, "error.gen.info.visible.null"),
    GEN_INFO_ORDER_NULL(1513, "error.gen.info.order.null"),
    GEN_INFO_FILTER_NULL(1514, "error.gen.info.filter.null"),
    GEN_INFO_FILTER_COMPONENT_NULL(1515, "error.gen.info.filter.component.null"),
    GEN_INFO_FILTER_TYPE_NULL(1516, "error.gen.info.filter.type.null"),
    GEN_INFO_FORM_COMPONENT_NULL(1517, "error.gen.info.form.component.null"),
    GEN_INFO_FORM_VISIBLE_NULL(1518, "error.gen.info.form.visible.null"),
    GEN_INFO_FORM_REQUIRED_NULL(1519, "error.gen.info.form.required.null"),
    GEN_INFO_AGGREGATE_NULL(1520, "error.gen.info.aggregate.null"),
    GEN_BIND_COMPONENT_TYPE_NULL(1521, "error.gen.bind.component.type.null"),
    GEN_INFO_DATABASE_NAME_NULL(1522, "error.gen.info.database.name.null"),
    GEN_AGGREGATE_DOMAIN_MASK_NULL(1523, "error.gen.aggregate.domain.mask.null"),
    GEN_AGGREGATE_OBJECT_NAME_NULL(1524, "error.gen.aggregate.object.name.null"),
    GEN_AGGREGATE_OBJECT_TYPE_NULL(1525, "error.gen.aggregate.object.type.null"),
    GEN_TEMPLATE_NAME_NULL(1526, "error.gen.template.name.null"),
    GEN_TEMPLATE_CONTENT_NULL(1527, "error.gen.template.content.null"),
    GEN_BIND_TYPESCRIPT_TYPE_NULL(1528, "error.gen.bind.typescript.type.null"),
    GEN_I18N_EN_NULL(1529, "error.gen.i18n.en.null"),
    GEN_I18N_LOCALE_NULL(1530, "error.gen.i18n.locale.null"),
    GEN_INFO_MODULE_NAME_NULL(1531, "error.gen.info.module.name.null"),
    GEN_INFO_PROJECT_NAME_NULL(1532, "error.gen.info.project.name.null"),

    POST_CODE_NULL(1600, "error.post.code.null"),
    POST_NAME_NULL(1601, "error.post.name.null"),
    POST_SORT_ORDER_NULL(1602, "error.post.sortOrder.null"),
    POST_STATUS_NULL(1603, "error.post.status.null"),
    POST_ID_NULL(1604, "error.post.id.null"),

    USER_POST_USER_ID_NULL(1700, "error.userPost.userId.null"),
    USER_POST_POST_ID_NULL(1701, "error.userPost.postId.null"),

    ROLE_MENU_DATA_SCOPE_ROLE_ID_NULL(1800, "error.roleMenuDataScope.roleId.null"),
    ROLE_MENU_DATA_SCOPE_MENU_ID_NULL(1801, "error.roleMenuDataScope.menuId.null"),
    ROLE_MENU_DATA_SCOPE_NULL(1802, "error.roleMenuDataScope.dataScope.null"),

    PERMISSION_ROLE_ID_NULL(1803, "error.permission.roleId.null"),
    PERMISSION_MENU_ID_NULL(1804, "error.permission.menuId.null"),
    PERMISSION_SCOPE_TYPE_NULL(1805, "error.permission.scopeType.null"),
    PERMISSION_SCOPE_TYPE_INVALID(1806, "error.permission.scopeType.invalid"),

    EVENT_APPLY_METHOD_NOT_FOUND(1900, "error.event.applyMethod.notFound"),
    EVENT_APPLY_METHOD_INVOKE_FAILED(1901, "error.event.applyMethod.invokeFailed"),
    EVENT_TYPE_NOT_REGISTERED(1902, "error.event.type.notRegistered"),
    EVENT_ENTITY_ID_NULL(1903, "error.event.entityId.null"),
    EVENT_JSON_SERIALIZE_FAILED(1904, "error.event.json.serializeFailed"),
    EVENT_JSON_DESERIALIZE_FAILED(1905, "error.event.json.deserializeFailed"),

    ;

    private final int code;
    private final String messageKey;
}

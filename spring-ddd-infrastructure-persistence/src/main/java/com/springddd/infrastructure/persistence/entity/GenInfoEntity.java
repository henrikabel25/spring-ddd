package com.springddd.infrastructure.persistence.entity;

import com.springddd.domain.util.IdGenerate;
import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("gen_info")
public class GenInfoEntity {

    @Id
    @IdGenerate
    private Long id;

    private String tableName;

    private String packageName;

    private String className;

    private String requestName;

    private Byte propValueObject;

    private String propColumnKey;

    private String propColumnName;

    private String propColumnType;

    private String propColumnComment;

    private String propJavaEntity;

    private String propJavaType;

    private Long propDictId;

    private Byte tableVisible;

    private Byte tableOrder;

    private Byte tableFilter;

    private Integer tableFilterComponent;

    private Integer tableFilterType;

    private Integer formComponent;

    private Byte formVisible;

    private Byte formRequired;

    private Boolean deleteStatus;

    @CreatedBy
    private String createBy;

    @CreatedDate
    private LocalDateTime createTime;

    @LastModifiedBy
    private String updateBy;

    @LastModifiedDate
    private LocalDateTime updateTime;

    @Version
    private Integer version;
}

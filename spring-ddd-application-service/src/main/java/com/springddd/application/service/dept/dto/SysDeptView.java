package com.springddd.application.service.dept.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SysDeptView implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    private String deptName;

    private Integer sortOrder;

    private Boolean deptStatus;

    private Boolean deleteStatus;

    private List<SysDeptView> children;
}

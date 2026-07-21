package com.springddd.application.service.post.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SysPostView implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String postCode;

    private String postName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    private Integer sortOrder;

    private Boolean postStatus;

    private Boolean deleteStatus;

    private List<SysPostView> children;
}

package com.springddd.application.service.user.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;

@Data
public class SysUserView implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String username;

    private String password;

    private String phone;

    private String avatar;

    private String email;

    private Boolean sex;

    private Boolean lockStatus;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long deptId;

    private Boolean deleteStatus;

    private Integer version;
}

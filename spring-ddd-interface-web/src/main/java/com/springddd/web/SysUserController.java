package com.springddd.web;

import com.springddd.application.service.user.SysUserCommandService;
import com.springddd.application.service.user.SysUserQueryService;
import com.springddd.application.service.user.dto.SysUserCommand;
import com.springddd.application.service.user.dto.SysUserQuery;
import com.springddd.domain.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/sys/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserCommandService sysUserCommandService;

    private final SysUserQueryService sysUserQueryService;

    @PostMapping("/index")
    public Mono<ApiResponse> page(@RequestBody @Validated Mono<SysUserQuery> query) {
        return ApiResponse.validated(query, sysUserQueryService::page);
    }

    @PostMapping("/recycle")
    public Mono<ApiResponse> recyclePage(@RequestBody @Validated Mono<SysUserQuery> query) {
        return ApiResponse.validated(query, sysUserQueryService::recycle);
    }

    @PostMapping("/create")
    public Mono<ApiResponse> create(@RequestBody SysUserCommand command) {
        return ApiResponse.ok(sysUserCommandService.createUser(command));
    }

    @PutMapping("/update")
    public Mono<ApiResponse> update(@RequestBody SysUserCommand command) {
        return ApiResponse.ok(sysUserCommandService.updateUser(command));
    }

    @PostMapping("/delete")
    public Mono<ApiResponse> delete(@RequestBody SysUserCommand command) {
        return ApiResponse.ok(sysUserCommandService.deleteUser(command));
    }

    @DeleteMapping("/wipe")
    public Mono<ApiResponse> wipe(@RequestParam("ids") List<Long> ids) {
        return ApiResponse.ok(sysUserCommandService.wipe(ids));
    }
}

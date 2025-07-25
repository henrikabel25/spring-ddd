package com.springddd.web;

import com.springddd.application.service.dept.SysDeptCommandService;
import com.springddd.application.service.dept.SysDeptQueryService;
import com.springddd.application.service.dept.dto.SysDeptCommand;
import com.springddd.application.service.dept.dto.SysDeptQuery;
import com.springddd.domain.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/sys/dept")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptQueryService sysDeptQueryService;

    private final SysDeptCommandService sysDeptCommandService;

    @PostMapping("/index")
    public Mono<ApiResponse> index(@RequestBody Mono<SysDeptQuery> query) {
        return ApiResponse.validated(query, sysDeptQueryService::index);
    }

    @PostMapping("/recycle")
    public Mono<ApiResponse> recycle(@RequestBody Mono<SysDeptQuery> query) {
        return ApiResponse.validated(query, sysDeptQueryService::recycle);
    }

    @PostMapping("/tree")
    public Mono<ApiResponse> tree() {
        return ApiResponse.ok(sysDeptQueryService.deptTree());
    }

    @PostMapping("/create")
    public Mono<ApiResponse> create(@RequestBody SysDeptCommand command) {
        return ApiResponse.ok(sysDeptCommandService.create(command));
    }

    @PutMapping("/update")
    public Mono<ApiResponse> update(@RequestBody SysDeptCommand command) {
        return ApiResponse.ok(sysDeptCommandService.update(command));
    }

    @PostMapping("/delete")
    public Mono<ApiResponse> delete(@RequestParam("ids") List<Long> ids) {
        return ApiResponse.ok(sysDeptCommandService.delete(ids));
    }

    @PostMapping("/restore")
    public Mono<ApiResponse> restore(@RequestParam("ids") List<Long> ids) {
        return ApiResponse.ok(sysDeptCommandService.restore(ids));
    }

    @DeleteMapping("/wipe")
    public Mono<ApiResponse> wipe(@RequestParam("ids") List<Long> ids) {
        return ApiResponse.ok(sysDeptCommandService.wipe(ids));
    }
}

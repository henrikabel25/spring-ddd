package com.springddd.web;

import com.springddd.application.service.user.SysUserPostCommandService;
import com.springddd.application.service.user.SysUserPostQueryService;
import com.springddd.application.service.user.dto.SysUserPostPageQuery;
import com.springddd.domain.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sys/userPost")
@RequiredArgsConstructor
public class SysUserPostController {

    private final SysUserPostCommandService sysUserPostCommandService;

    private final SysUserPostQueryService sysUserPostQueryService;

    @GetMapping
    public Mono<ApiResponse> list(@RequestParam Long userId) {
        return ApiResponse.ok(sysUserPostQueryService.listByUserId(userId));
    }

    @PostMapping("/index")
    public Mono<ApiResponse> index(@RequestHeader(value = "X-Menu-Id", required = false) Long menuId,
                                       @RequestBody @Validated Mono<SysUserPostPageQuery> query) {
        return ApiResponse.validated(query, q -> sysUserPostQueryService.index(menuId, q));
    }

    @PostMapping("/recycle")
    public Mono<ApiResponse> recycle(@RequestHeader(value = "X-Menu-Id", required = false) Long menuId,
                                         @RequestBody @Validated Mono<SysUserPostPageQuery> query) {
        return ApiResponse.validated(query, q -> sysUserPostQueryService.recycle(menuId, q));
    }

    @PostMapping("/batch")
    public Mono<ApiResponse> batchSave(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        List<?> rawPostIds = (List<?>) body.get("postIds");
        List<Long> postIds = rawPostIds == null ? List.of()
                : rawPostIds.stream().map(o -> Long.valueOf(o.toString())).toList();
        return ApiResponse.ok(sysUserPostCommandService.batchSave(userId, postIds));
    }
}

package com.example.resilience.web;

import com.example.resilience.audit.Audited;
import com.example.resilience.common.ApiResponse;
import com.example.resilience.domain.ReliefRequest;
import com.example.resilience.service.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/relief-requests")
public class ReliefRequestController {
    private final ReliefRequestService service;
    private final AllocationService allocation;
    private final IdempotencyService idempotency;
    public ReliefRequestController(ReliefRequestService service, AllocationService allocation, IdempotencyService idempotency) {
        this.service = service; this.allocation = allocation; this.idempotency = idempotency;
    }

    @PostMapping
    @Audited(action = "CREATE_RELIEF_REQUEST", resource = "ReliefRequest")
    public ApiResponse<RequestView> create(@RequestHeader("Idempotency-Key") String key,
                                           @Valid @RequestBody ReliefRequestService.CreateCommand body,
                                           Authentication authentication) {
        if (key.isBlank() || key.length() > 100) {
            throw new com.example.resilience.common.BusinessException("INVALID_ARGUMENT", "幂等键长度应为 1 至 100", org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        var replay = service.replay(authentication.getName(), key, body);
        if (replay.isPresent()) return ApiResponse.ok(view(replay.get()));
        try (var lease = idempotency.acquire(authentication.getName(), key)) {
            ReliefRequest created = service.create(authentication.getName(), key, body);
            lease.commit(created.getRequestNo());
            return ApiResponse.ok(view(created));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // The failed service transaction is rolled back before this second read.
            var concurrent = service.replay(authentication.getName(), key, body);
            if (concurrent.isPresent()) return ApiResponse.ok(view(concurrent.get()));
            throw e;
        }
    }

    @GetMapping("/{requestNo}")
    public ApiResponse<RequestView> get(@PathVariable String requestNo, Authentication authentication) {
        return ApiResponse.ok(view(service.getVisible(requestNo, authentication.getName(), isAdmin(authentication))));
    }

    @GetMapping
    public ApiResponse<PageView> page(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String keyword,
            @RequestParam(required = false) com.example.resilience.domain.Enums.RequestStatus status,
            Authentication authentication) {
        if (page < 0 || page > 10000 || size < 1 || size > 100 || (keyword != null && keyword.length() > 40)) {
            throw new com.example.resilience.common.BusinessException("INVALID_ARGUMENT", "分页参数或编号长度不合法", org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        Pageable safe = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        var result = service.search(authentication.getName(), isAdmin(authentication), keyword, status, safe);
        return ApiResponse.ok(new PageView(result.getContent().stream().map(ReliefRequestController::view).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @PostMapping("/{requestNo}/allocate")
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(action = "ALLOCATE_SUPPLIES", resource = "ReliefRequest")
    public ApiResponse<AllocationService.Result> allocate(@PathVariable String requestNo) {
        return ApiResponse.ok(allocation.allocate(requestNo));
    }

    private static RequestView view(ReliefRequest r) {
        return new RequestView(r.getRequestNo(), r.getDistrict(), r.getCategory().name(), r.getQuantity(),
                r.getPriority().name(), r.getStatus().name(), r.getCreatedAt());
    }
    public record RequestView(String requestNo, String district, String category, int quantity,
                              String priority, String status, Instant createdAt) {}
    public record PageView(List<RequestView> content, int number, int size, long totalElements, int totalPages) {}
}

package com.example.resilience.service;

import com.example.resilience.common.BusinessException;
import com.example.resilience.domain.*;
import com.example.resilience.repository.*;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.resilience.domain.Enums.*;

@Service
public class ReliefRequestService {
    private final ReliefRequestRepository requests;
    private final UserRepository users;

    public ReliefRequestService(ReliefRequestRepository requests, UserRepository users) {
        this.requests = requests; this.users = users;
    }

    @Transactional
    public ReliefRequest create(String username, CreateCommand command) {
        return create(username, null, command);
    }

    @Transactional
    public ReliefRequest create(String username, String key, CreateCommand command) {
        AppUser user = users.findByUsername(username).orElseThrow(() -> BusinessException.notFound("用户不存在"));
        String requestNo = "RR" + java.time.LocalDate.now().toString().replace("-", "")
                + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        ReliefRequest request = new ReliefRequest(requestNo, user.getId(), command.district(),
                command.category(), command.quantity(), command.priority());
        request.setIdempotencyKey(key);
        return requests.saveAndFlush(request);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<ReliefRequest> replay(String username, String key, CreateCommand command) {
        Long userId = userId(username);
        return requests.findByRequesterIdAndIdempotencyKey(userId, key).map(existing -> {
            if (!existing.getDistrict().equals(command.district()) || existing.getCategory() != command.category()
                    || existing.getQuantity() != command.quantity() || existing.getPriority() != command.priority()) {
                throw BusinessException.conflict("同一幂等键不能用于不同的请求内容");
            }
            return existing;
        });
    }

    private Long userId(String username) {
        return users.findByUsername(username).orElseThrow(() -> BusinessException.notFound("用户不存在")).getId();
    }

    @Transactional(readOnly = true)
    public ReliefRequest getVisible(String requestNo, String username, boolean admin) {
        ReliefRequest request = get(requestNo);
        if (!admin && !request.getRequesterId().equals(userId(username))) {
            // Do not disclose whether another user's identifier exists.
            throw BusinessException.notFound("求助单不存在");
        }
        return request;
    }

    @Transactional(readOnly = true)
    public Page<ReliefRequest> search(String username, boolean admin, String keyword,
                                     RequestStatus status, Pageable pageable) {
        Long ownerId = admin ? null : userId(username);
        return requests.findAll((root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> terms = new java.util.ArrayList<>();
            if (ownerId != null) terms.add(cb.equal(root.get("requesterId"), ownerId));
            if (status != null) terms.add(cb.equal(root.get("status"), status));
            if (keyword != null && !keyword.isBlank()) {
                String escaped = keyword.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_");
                terms.add(cb.like(root.get("requestNo"), "%" + escaped + "%", '!'));
            }
            return cb.and(terms.toArray(jakarta.persistence.criteria.Predicate[]::new));
        }, pageable);
    }

    @Transactional(readOnly = true)
    public ReliefRequest get(String requestNo) {
        return requests.findByRequestNo(requestNo).orElseThrow(() -> BusinessException.notFound("求助单不存在"));
    }

    @Transactional(readOnly = true)
    public Page<ReliefRequest> page(Pageable pageable) { return requests.findAll(pageable); }

    public record CreateCommand(
            @NotBlank @Size(max = 50) String district,
            @NotNull SupplyCategory category,
            @Min(1) @Max(10000) int quantity,
            @NotNull Priority priority) {}
}

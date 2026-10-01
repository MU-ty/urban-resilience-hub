package com.example.resilience.audit;

import com.example.resilience.domain.AuditLog;
import com.example.resilience.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Aspect
@Component
public class AuditAspect {
    private final AuditLogRepository audits;
    private final HttpServletRequest request;
    public AuditAspect(AuditLogRepository audits, HttpServletRequest request) { this.audits = audits; this.request = request; }

    @AfterReturning("@annotation(audited)")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(JoinPoint joinPoint, Audited audited) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String actor = authentication == null ? "anonymous" : authentication.getName();
        String detail = "{\"method\":\"" + joinPoint.getSignature().toShortString() + "\"}";
        audits.save(new AuditLog(actor, audited.action(), audited.resource(), null,
                request.getRemoteAddr(), detail));
    }
}

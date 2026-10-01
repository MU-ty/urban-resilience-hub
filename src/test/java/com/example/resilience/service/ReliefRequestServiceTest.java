package com.example.resilience.service;

import com.example.resilience.domain.*;
import com.example.resilience.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static com.example.resilience.domain.Enums.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReliefRequestServiceTest {
    ReliefRequestRepository requests = mock(ReliefRequestRepository.class);
    UserRepository users = mock(UserRepository.class);
    ReliefRequestService service = new ReliefRequestService(requests, users);
    ReliefRequest request = new ReliefRequest("RR1", 1L, "浦东新区", SupplyCategory.WATER, 20, Priority.NORMAL);
    @BeforeEach void init() {
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(2L);
        when(users.findByUsername("citizen")).thenReturn(Optional.of(user));
        when(requests.findByRequestNo("RR1")).thenReturn(Optional.of(request));
    }
    @Test void rejectsOtherUsersDetail() {
        assertThatThrownBy(() -> service.getVisible("RR1", "citizen", false)).hasMessage("求助单不存在");
    }
    @Test void adminCanReadAnyDetail() {
        assertThat(service.getVisible("RR1", "admin", true)).isSameAs(request);
        verifyNoInteractions(users);
    }
    @Test void ownerCanReadDetail() {
        var own = new ReliefRequest("RR2", 2L, "浦东新区", SupplyCategory.WATER, 20, Priority.NORMAL);
        when(requests.findByRequestNo("RR2")).thenReturn(Optional.of(own));
        assertThat(service.getVisible("RR2", "citizen", false)).isSameAs(own);
    }
    @Test void sameKeyAndBodyReturnsExistingRequest() {
        when(requests.findByRequesterIdAndIdempotencyKey(2L,"key")).thenReturn(Optional.of(request));
        assertThat(service.replay("citizen", "key", new ReliefRequestService.CreateCommand("浦东新区", SupplyCategory.WATER, 20, Priority.NORMAL))).contains(request);
    }
    @Test void sameKeyWithDifferentBodyConflicts() {
        when(requests.findByRequesterIdAndIdempotencyKey(2L,"key")).thenReturn(Optional.of(request));
        assertThatThrownBy(() -> service.replay("citizen", "key", new ReliefRequestService.CreateCommand("浦东新区", SupplyCategory.WATER, 21, Priority.NORMAL)))
                .hasMessageContaining("不同的请求内容");
    }
    @Test void createPersistsIdempotencyKeyAndOwner() {
        when(requests.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var created = service.create("citizen", "unique-key", new ReliefRequestService.CreateCommand("浦东新区", SupplyCategory.WATER, 20, Priority.NORMAL));
        assertThat(created.getIdempotencyKey()).isEqualTo("unique-key");
        assertThat(created.getRequesterId()).isEqualTo(2L);
        assertThat(created.getStatus()).isEqualTo(RequestStatus.PENDING);
    }
}

package com.example.resilience.service;

import com.example.resilience.domain.*;
import com.example.resilience.repository.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static com.example.resilience.domain.Enums.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AllocationServiceTest {
    @Test void locksRequestBeforeCheckingStateAndNeverAllocatesTwice() {
        var requests = mock(ReliefRequestRepository.class);
        var lots = mock(SupplyLotRepository.class);
        var allocations = mock(AllocationRepository.class);
        var outbox = mock(OutboxRepository.class);
        var request = new ReliefRequest("RR1",1L,"象山区",SupplyCategory.WATER,20,Priority.NORMAL);
        request.allocated(true);
        when(requests.findForUpdate("RR1")).thenReturn(Optional.of(request));
        assertThatThrownBy(() -> new AllocationService(requests,lots,allocations,outbox).allocate("RR1"))
                .hasMessageContaining("已分配");
        verify(requests).findForUpdate("RR1");
        verifyNoInteractions(lots, allocations, outbox);
    }
}

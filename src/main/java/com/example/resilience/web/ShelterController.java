package com.example.resilience.web;

import com.example.resilience.common.ApiResponse;
import com.example.resilience.service.ShelterQueryService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/shelters")
public class ShelterController {
    private final ShelterQueryService service;
    public ShelterController(ShelterQueryService service) { this.service = service; }

    @GetMapping
    public ApiResponse<List<ShelterQueryService.ShelterView>> byDistrict(
            @RequestParam @NotBlank @Size(max = 50) String district) {
        return ApiResponse.ok(service.byDistrict(district));
    }
}

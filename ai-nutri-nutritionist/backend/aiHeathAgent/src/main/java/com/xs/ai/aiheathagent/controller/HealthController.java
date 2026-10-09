package com.xs.ai.aiheathagent.controller;


import com.xs.ai.aiheathagent.common.ApiResp;
import com.xs.ai.aiheathagent.dto.req.*;
import com.xs.ai.aiheathagent.dto.resp.*;
import com.xs.ai.aiheathagent.repo.UserRepo;
import com.xs.ai.aiheathagent.service.HealthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;


@RestController @RequestMapping("/api/health")
public class HealthController extends BaseController {
    private final HealthService healthService;
    public HealthController(UserRepo userRepo, HealthService healthService){ super(userRepo); this.healthService = healthService; }


    @GetMapping("/overview")
    public ApiResp<HealthOverviewResp> overview(){ var u = current(); return ApiResp.ok(healthService.overview(u.getId())); }


    @GetMapping("/weight/trend")
    public ApiResp<WeightTrendResp> weightTrend(@RequestParam(defaultValue = "week") String range){ var u = current(); return ApiResp.ok(healthService.weightTrend(u.getId(), range)); }


    @GetMapping("/records")
    public ApiResp<PageResp<RecordItemResp>> list(@RequestParam(required=false) String type, @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int pageSize){ var u = current(); return ApiResp.ok(healthService.listRecords(u.getId(), type, page, pageSize)); }


    @PostMapping("/records")
    public ApiResp<RecordItemResp> create(@Valid @RequestBody RecordCreateReq req){ var u = current(); return ApiResp.ok(healthService.createRecord(u.getId(), req)); }


    @PutMapping("/records/{id}")
    public ApiResp<RecordItemResp> update(@PathVariable Long id, @Valid @RequestBody RecordUpdateReq req){ var u = current(); req.setId(id); return ApiResp.ok(healthService.updateRecord(u.getId(), id, req)); }


    @DeleteMapping("/records/{id}")
    public ApiResp<?> delete(@PathVariable Long id){ var u = current(); healthService.deleteRecord(u.getId(), id); return ApiResp.ok(java.util.Map.of()); }


    @GetMapping("/profile")
    public ApiResp<ProfileResp> profile(){ var u = current(); return ApiResp.ok(healthService.getProfile(u.getId())); }


    @PostMapping("/profile")
    public ApiResp<?> updateProfile(@RequestBody ProfileUpdateReq req){ var u = current(); healthService.updateProfile(u.getId(), req); return ApiResp.ok(java.util.Map.of()); }

    @GetMapping("/space")
    public ApiResp<HealthSpaceViewResp> space(@RequestParam(defaultValue = "0") int viewIndex){ var u = current(); return ApiResp.ok(healthService.healthSpaceView(u.getId(), viewIndex)); }
}

package com.xs.ai.aiheathagent.controller;

import com.xs.ai.aiheathagent.common.ApiResp;
import com.xs.ai.aiheathagent.conversation.service.SummaryReportService;
import com.xs.ai.aiheathagent.dto.req.SummaryRunReq;
import com.xs.ai.aiheathagent.entity.DailySummaryRecord;
import com.xs.ai.aiheathagent.repo.UserRepo;
import com.xs.ai.aiheathagent.repository.DailySummaryRecordRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("")
@CrossOrigin(origins = "*")
public class SummaryController extends BaseController {
    private final SummaryReportService summaryReportService;
    private final DailySummaryRecordRepository dailySummaryRecordRepository;

    public SummaryController(
            SummaryReportService summaryReportService,
            DailySummaryRecordRepository dailySummaryRecordRepository,
            UserRepo userRepo
    ) {
        super(userRepo);
        this.summaryReportService = summaryReportService;
        this.dailySummaryRecordRepository = dailySummaryRecordRepository;
    }

    @GetMapping({"/api/summary/today", "/api/summary/today/", "/api/api/summary/today", "/api/api/summary/today/", "/summary/today", "/summary/today/"})
    public ApiResp<Map<String, Object>> today() {
        var user = current();
        Long userId = user.getId();
        LocalDate date = LocalDate.now();
        DailySummaryRecord rec = dailySummaryRecordRepository.findTopByUserIdAndDateOrderByIdDesc(userId, date).orElse(null);
        return ApiResp.ok(Map.of(
                "date", date.toString(),
                "exists", rec != null,
                "userText", rec == null ? "" : String.valueOf(rec.getUserText()),
                "explanationCode", rec == null || rec.getExplanationCode() == null ? "" : rec.getExplanationCode().name(),
                "explanationArgs", rec == null ? null : rec.getExplanationArgs()
        ));
    }

    @PostMapping({"/api/summary/run", "/api/summary/run/", "/api/api/summary/run", "/api/api/summary/run/", "/summary/run", "/summary/run/"})
    public ApiResp<Map<String, Object>> run(@RequestBody(required = false) SummaryRunReq req) {
        var user = current();
        LocalDate date = LocalDate.now();
        try {
            String d = String.valueOf(req == null ? "" : (req.getDate() == null ? "" : req.getDate())).trim();
            if (!d.isEmpty()) {
                date = LocalDate.parse(d);
            }
        } catch (Exception ignored) {
            date = LocalDate.now();
        }
        String trigger = String.valueOf(req == null ? "" : (req.getTrigger() == null ? "" : req.getTrigger())).trim();
        if (trigger.isEmpty()) {
            trigger = "on_demand";
        }
        var out = summaryReportService.generateAndPersistSync(user, date, trigger);
        String userText = out == null || out.userFacing() == null ? "" : String.valueOf(out.userFacing().text());
        return ApiResp.ok(Map.of("ok", true, "date", date.toString(), "trigger", trigger, "userText", userText));
    }
}


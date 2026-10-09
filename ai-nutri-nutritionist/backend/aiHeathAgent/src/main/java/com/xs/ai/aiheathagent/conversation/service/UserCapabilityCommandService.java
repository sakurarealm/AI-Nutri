package com.xs.ai.aiheathagent.conversation.service;

import com.xs.ai.aiheathagent.entity.CapabilityKey;
import com.xs.ai.aiheathagent.entity.CapabilityState;
import com.xs.ai.aiheathagent.entity.UserCapabilityChangeRecord;
import com.xs.ai.aiheathagent.entity.UserCapabilityProfile;
import com.xs.ai.aiheathagent.repository.UserCapabilityChangeRecordRepository;
import com.xs.ai.aiheathagent.repository.UserCapabilityProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserCapabilityCommandService {
    private final UserCapabilityProfileRepository profileRepository;
    private final UserCapabilityChangeRecordRepository changeRepository;

    public UserCapabilityCommandService(
            UserCapabilityProfileRepository profileRepository,
            UserCapabilityChangeRecordRepository changeRepository
    ) {
        this.profileRepository = profileRepository;
        this.changeRepository = changeRepository;
    }

    @Transactional
    public UserCapabilityProfile initIfAbsent(Long userId, String source, String reason) {
        if (userId == null) {
            throw new IllegalArgumentException("userId required");
        }
        return profileRepository.findByUserId(userId).orElseGet(() -> {
            UserCapabilityProfile p = new UserCapabilityProfile();
            p.setUserId(userId);
            p.setUpdatedAt(LocalDateTime.now());
            UserCapabilityProfile saved = profileRepository.save(p);
            appendChange(userId, CapabilityKey.PROACTIVE_REMINDER, CapabilityState.OFF, CapabilityState.OFF, source, reason);
            return saved;
        });
    }

    @Transactional
    public UserCapabilityProfile suggest(Long userId, CapabilityKey key, String source, String reason) {
        UserCapabilityProfile p = initIfAbsent(userId, source, reason);
        CapabilityState from = getState(p, key);
        if (from != CapabilityState.OFF) {
            return p;
        }
        setState(p, key, CapabilityState.SUGGESTED);
        p.setUpdatedAt(LocalDateTime.now());
        UserCapabilityProfile saved = profileRepository.save(p);
        appendChange(userId, key, from, CapabilityState.SUGGESTED, source, reason);
        return saved;
    }

    @Transactional
    public UserCapabilityProfile enable(Long userId, CapabilityKey key, String source, String reason) {
        UserCapabilityProfile p = initIfAbsent(userId, source, reason);
        CapabilityState from = getState(p, key);
        if (from == CapabilityState.ENABLED) {
            return p;
        }
        setState(p, key, CapabilityState.ENABLED);
        p.setUpdatedAt(LocalDateTime.now());
        UserCapabilityProfile saved = profileRepository.save(p);
        appendChange(userId, key, from, CapabilityState.ENABLED, source, reason);
        return saved;
    }

    @Transactional
    public UserCapabilityProfile disable(Long userId, CapabilityKey key, String source, String reason) {
        UserCapabilityProfile p = initIfAbsent(userId, source, reason);
        CapabilityState from = getState(p, key);
        if (from == CapabilityState.OFF) {
            return p;
        }
        setState(p, key, CapabilityState.OFF);
        p.setUpdatedAt(LocalDateTime.now());
        UserCapabilityProfile saved = profileRepository.save(p);
        appendChange(userId, key, from, CapabilityState.OFF, source, reason);
        return saved;
    }

    private void appendChange(Long userId, CapabilityKey key, CapabilityState from, CapabilityState to, String source, String reason) {
        UserCapabilityChangeRecord rec = new UserCapabilityChangeRecord();
        rec.setUserId(userId);
        rec.setCapabilityKey(key);
        rec.setFromState(from == null ? CapabilityState.OFF : from);
        rec.setToState(to == null ? CapabilityState.OFF : to);
        rec.setSource(String.valueOf(source == null ? "" : source).trim().isEmpty() ? "unknown" : source.trim());
        rec.setReason(String.valueOf(reason == null ? "" : reason));
        changeRepository.save(rec);
    }

    private CapabilityState getState(UserCapabilityProfile p, CapabilityKey key) {
        if (p == null || key == null) {
            return CapabilityState.OFF;
        }
        return switch (key) {
            case MEDICATION_REMINDER -> safe(p.getMedicationReminderEnabled());
            case SUPPLEMENT_REMINDER -> safe(p.getSupplementReminderEnabled());
            case EXERCISE_SUGGESTION -> safe(p.getExerciseSuggestionEnabled());
            case PROACTIVE_SUMMARY -> safe(p.getProactiveSummaryEnabled());
            case PROACTIVE_REMINDER -> safe(p.getProactiveReminderEnabled());
        };
    }

    private void setState(UserCapabilityProfile p, CapabilityKey key, CapabilityState state) {
        if (p == null || key == null) {
            return;
        }
        CapabilityState s = safe(state);
        switch (key) {
            case MEDICATION_REMINDER -> p.setMedicationReminderEnabled(s);
            case SUPPLEMENT_REMINDER -> p.setSupplementReminderEnabled(s);
            case EXERCISE_SUGGESTION -> p.setExerciseSuggestionEnabled(s);
            case PROACTIVE_SUMMARY -> p.setProactiveSummaryEnabled(s);
            case PROACTIVE_REMINDER -> p.setProactiveReminderEnabled(s);
        }
    }

    private CapabilityState safe(CapabilityState s) {
        return s == null ? CapabilityState.OFF : s;
    }
}


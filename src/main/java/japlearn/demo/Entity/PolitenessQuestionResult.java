package japlearn.demo.Entity;

import java.util.List;

/** Observed responses only: older totals cannot reconstruct these distinctions. */
public record PolitenessQuestionResult(String scenarioId, String relationship, String learnerRole,
    String prompt, String npcLine, String selectedJapanese, List<String> acceptedJapanese,
    boolean correct, boolean hintUsed, String recordedAt) {}

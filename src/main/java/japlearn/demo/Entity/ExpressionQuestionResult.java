package japlearn.demo.Entity;

/** Per-question evidence; technical interruptions are never learning errors. */
public record ExpressionQuestionResult(
    String questionId, String expression, int questionIndex, String mode, String outcome,
    String selectedPosition, String correctPosition, String selectedScenario,
    String correctScenario, String alternativeScenario, boolean hintUsed,
    int audioReplays, String recordedAt
) {}

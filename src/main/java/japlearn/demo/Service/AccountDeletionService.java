package japlearn.demo.Service;

import java.util.List;
import java.util.Locale;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import japlearn.demo.Entity.DialogueRelayBonusAssessment;
import japlearn.demo.Entity.GuidedPhraseDailyPractice;
import japlearn.demo.Entity.LessonQuizAttempt;
import japlearn.demo.Entity.QuackTalkSession;
import japlearn.demo.Entity.ReplyCoachAttempt;
import japlearn.demo.Entity.ResponseRushProgress;
import japlearn.demo.Entity.Score;
import japlearn.demo.Entity.SituationalAttempt;
import japlearn.demo.Entity.SituationalRun;
import japlearn.demo.Entity.Student;
import japlearn.demo.Entity.StudentProgress;
import japlearn.demo.Entity.User;

/**
 * Permanently removes a learner account and every record stored against it.
 *
 * Required by Google Play: an app that lets people create an account must also
 * let them delete it together with the data it collected.
 */
@Service
public class AccountDeletionService {

    /** Every collection that stores learner records under an "email" field. */
    private static final List<Class<?>> BY_EMAIL = List.of(
            StudentProgress.class,
            Score.class,
            QuackTalkSession.class,
            ReplyCoachAttempt.class,
            ResponseRushProgress.class,
            SituationalAttempt.class,
            SituationalRun.class,
            GuidedPhraseDailyPractice.class,
            DialogueRelayBonusAssessment.class,
            Student.class);

    private final MongoTemplate mongo;

    public AccountDeletionService(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    /**
     * Deletes the learner's records and finally the sign-in account itself.
     *
     * @return the number of documents removed, for the audit log.
     */
    public long deleteLearnerAccount(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return 0;
        }

        long removed = 0;

        // Learner records are matched case-insensitively, the same way they are read.
        Query byEmail = new Query(Criteria.where("email").regex("^" + java.util.regex.Pattern.quote(normalized) + "$", "i"));
        for (Class<?> collection : BY_EMAIL) {
            removed += mongo.remove(byEmail, collection).getDeletedCount();
        }

        // Lesson quiz attempts store the learner under a different field name.
        Query byStudentEmail = new Query(
                Criteria.where("studentEmail").regex("^" + java.util.regex.Pattern.quote(normalized) + "$", "i"));
        removed += mongo.remove(byStudentEmail, LessonQuizAttempt.class).getDeletedCount();

        // The sign-in account goes last, so a failure above never leaves an
        // orphaned set of records behind an already-deleted login.
        removed += mongo.remove(byEmail, User.class).getDeletedCount();

        return removed;
    }
}

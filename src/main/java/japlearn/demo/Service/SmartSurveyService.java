package japlearn.demo.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import japlearn.demo.Entity.SmartSurveyUpload;
import japlearn.demo.Entity.User;
import japlearn.demo.Repository.SmartSurveyUploadRepository;
import japlearn.demo.Repository.UserRepository;

/**
 * Validates and stores Google Forms exports of the Learning Effectiveness questionnaire
 * and computes Agree / Strongly Agree percentages. Survey results are kept apart from
 * game accuracy and are never merged into it.
 */
@Service
public class SmartSurveyService {

    public static final int QUESTIONS = 10;
    private static final int MAX_CHARS = 1_500_000;
    private static final int MAX_ROWS = 5_000;
    private static final int MAX_COLUMNS = 200;

    /** Q1-2, Q3-4, Q5-6, Q7-8, Q9-10. */
    public static final List<String[]> GROUPS = List.of(
            new String[] { "SMART1", "Situational communication" },
            new String[] { "SMART2", "Conversational response" },
            new String[] { "SMART3", "Politeness recognition" },
            new String[] { "SUPPORT", "Supporting communication familiarity" },
            new String[] { "SMART4", "Perceived activity completion and reinforcement" });

    private static final Set<String> AGREE = Set.of("strongly agree", "agree", "4", "5");
    private static final Set<String> OTHER = Set.of("neutral", "neither agree nor disagree", "neither agree or disagree",
            "undecided", "disagree", "strongly disagree", "1", "2", "3");

    private final SmartSurveyUploadRepository uploads;
    private final UserRepository users;

    public SmartSurveyService(SmartSurveyUploadRepository uploads, UserRepository users) {
        this.uploads = uploads;
        this.users = users;
    }

    public record UploadRequest(String fileName, String csv, List<Integer> questionColumns, Integer identifierColumn, Boolean matchedOnly) {}

    public SmartSurveyUpload upload(UploadRequest request, String uploadedBy) {
        if (request == null || request.csv() == null || request.csv().isBlank()) throw bad("The CSV file is empty.");
        String fileName = request.fileName() == null ? "" : request.fileName().trim();
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".csv")) throw bad("Please upload the .csv file exported from Google Forms.");
        if (request.csv().length() > MAX_CHARS) throw bad("The CSV file is too large (limit about 1.5 MB).");
        if (request.csv().indexOf('\0') >= 0) throw bad("This does not look like a text CSV file.");

        List<List<String>> table = parse(request.csv());
        if (table.size() < 2) throw bad("The CSV needs a header row and at least one response.");
        if (table.size() - 1 > MAX_ROWS) throw bad("The CSV has more than " + MAX_ROWS + " responses.");
        List<String> header = table.get(0);
        if (header.size() > MAX_COLUMNS) throw bad("The CSV has too many columns.");

        List<Integer> cols = request.questionColumns();
        if (cols == null || cols.size() != QUESTIONS) throw bad("Map exactly 10 Learning Effectiveness questions.");
        if (new HashSet<>(cols).size() != QUESTIONS) throw bad("Each question must use a different column.");
        for (Integer c : cols) if (c == null || c < 0 || c >= header.size()) throw bad("A mapped column does not exist in this CSV.");
        Integer idCol = request.identifierColumn();
        if (idCol != null && (idCol < 0 || idCol >= header.size() || cols.contains(idCol))) throw bad("The email column is not valid.");

        // A mapped column must mostly hold agreement answers; otherwise the mapping is wrong.
        for (int q = 0; q < QUESTIONS; q++) {
            int filled = 0, likert = 0;
            for (int r = 1; r < table.size(); r++) {
                String v = cell(table.get(r), cols.get(q));
                if (v.isBlank()) continue;
                filled++;
                if (scale(v) != null) likert++;
            }
            if (filled == 0 || likert * 2 < filled)
                throw bad("Question " + (q + 1) + " (\"" + clean(header.get(cols.get(q)), 80) + "\") does not contain Agree/Disagree answers. Check the column mapping.");
        }

        // Email only: the address given in the form is matched to the student account registered with it.
        // Names (and any student ID column) are ignored, so a different name on the form does not matter.
        Map<String, String> emailToId = new java.util.HashMap<>();
        for (User u : users.findByRoleIgnoreCase("student"))
            if (u.getId() != null && u.getEmail() != null) emailToId.put(u.getEmail().trim().toLowerCase(Locale.ROOT), u.getId());

        boolean matchedOnly = Boolean.TRUE.equals(request.matchedOnly()) && idCol != null;
        SmartSurveyUpload upload = new SmartSurveyUpload();
        upload.setFileName(clean(fileName, 120));
        upload.setSha256(sha256(request.csv()));
        upload.setUploadedBy(uploadedBy);
        upload.setIdentifierHeader(idCol == null ? null : clean(header.get(idCol), 200));
        upload.setMatchedOnly(matchedOnly);
        List<String> headers = new ArrayList<>();
        for (Integer c : cols) headers.add(clean(header.get(c), 300));
        upload.setQuestionHeaders(headers);

        Set<String> seen = new HashSet<>();
        int matched = 0, unmatched = 0, duplicate = 0, noId = 0, counted = 0, total = 0;
        for (int r = 1; r < table.size(); r++) {
            List<String> line = table.get(r);
            if (line.stream().allMatch(String::isBlank)) continue;
            total++;
            SmartSurveyUpload.Row row = new SmartSurveyUpload.Row();
            row.setRowNumber(r + 1);
            for (Integer c : cols) row.getAnswers().add(clean(cell(line, c), 60));
            String status;
            if (idCol == null || cell(line, idCol).isBlank()) {
                status = "NO_IDENTIFIER";
                noId++;
            } else {
                String raw = cell(line, idCol);
                String email;
                try { email = EmailInput.normalize(raw); } catch (RuntimeException e) { email = null; }
                String studentId = email == null ? null : emailToId.get(email);
                String key = email != null ? email : "raw:" + raw.trim().toLowerCase(Locale.ROOT);
                if (!seen.add(key)) { status = "DUPLICATE"; duplicate++; }
                else if (studentId != null) { status = "MATCHED"; matched++; row.setStudentId(studentId); }
                else { status = "UNMATCHED"; unmatched++; }
            }
            row.setIdentifierStatus(status);
            // Later submissions from the same verified email are flagged and left out (first one counts).
            boolean count = !status.equals("DUPLICATE") && (!matchedOnly || status.equals("MATCHED"));
            row.setCounted(count);
            if (count) counted++;
            upload.getRows().add(row);
        }
        if (total == 0) throw bad("The CSV has no responses.");
        upload.setTotalRows(total);
        upload.setCountedRows(counted);
        upload.setMatchedRows(matched);
        upload.setUnmatchedRows(unmatched);
        upload.setDuplicateRows(duplicate);
        upload.setNoIdentifierRows(noId);
        return uploads.save(upload);
    }

    public List<Map<String, Object>> summaries() {
        return uploads.findAllByOrderByUploadedAtDesc().stream().map(this::summary).toList();
    }

    /** Results are recomputed from the stored rows each time, so they are reproducible. */
    public Map<String, Object> summary(SmartSurveyUpload u) {
        List<Map<String, Object>> questions = new ArrayList<>();
        int[] agree = new int[QUESTIONS], valid = new int[QUESTIONS];
        for (SmartSurveyUpload.Row row : u.getRows()) {
            if (!row.isCounted()) continue;
            for (int q = 0; q < QUESTIONS && q < row.getAnswers().size(); q++) {
                Boolean a = scale(row.getAnswers().get(q));
                if (a == null) continue;
                valid[q]++;
                if (a) agree[q]++;
            }
        }
        for (int q = 0; q < QUESTIONS; q++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("number", q + 1);
            m.put("statement", q < u.getQuestionHeaders().size() ? u.getQuestionHeaders().get(q) : "");
            m.put("group", GROUPS.get(q / 2)[0]);
            m.put("agree", agree[q]);
            m.put("valid", valid[q]);
            m.put("percentage", valid[q] > 0 ? SmartGoalsService.round(agree[q] * 100.0 / valid[q]) : null);
            questions.add(m);
        }
        List<Map<String, Object>> groups = new ArrayList<>();
        for (int g = 0; g < GROUPS.size(); g++) {
            int a = agree[g * 2] + agree[g * 2 + 1], v = valid[g * 2] + valid[g * 2 + 1];
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", GROUPS.get(g)[0]);
            m.put("label", GROUPS.get(g)[1]);
            m.put("questions", "Q" + (g * 2 + 1) + "-Q" + (g * 2 + 2));
            m.put("agree", a);
            m.put("valid", v);
            m.put("percentage", v > 0 ? SmartGoalsService.round(a * 100.0 / v) : null);
            groups.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", u.getId());
        out.put("fileName", u.getFileName());
        out.put("sha256", u.getSha256());
        out.put("uploadedAt", u.getUploadedAt());
        out.put("uploadedBy", u.getUploadedBy());
        out.put("identifierHeader", u.getIdentifierHeader());
        out.put("matchedOnly", u.isMatchedOnly());
        out.put("totalRows", u.getTotalRows());
        out.put("countedRows", u.getCountedRows());
        out.put("matchedRows", u.getMatchedRows());
        out.put("unmatchedRows", u.getUnmatchedRows());
        out.put("duplicateRows", u.getDuplicateRows());
        out.put("noIdentifierRows", u.getNoIdentifierRows());
        out.put("questions", questions);
        out.put("groups", groups);
        return out;
    }

    private static final String[] GOAL_KEYS = { "SMART1", "SMART2", "SMART3", "SMART4" };
    /** Questionnaire group index (Q pairs) for each SMART goal: Q1-2, Q3-4, Q5-6, Q9-10. */
    private static final int[] GOAL_GROUP = { 0, 1, 2, 4 };
    private static final int[] GOAL_TARGET = { 80, 75, 75, 80 };

    /**
     * Per matched student: questionnaire agreement next to measured game performance for each goal,
     * with the difference (actual - survey) in percentage points. The two are never averaged.
     * Overall results sum the scored answers / assigned activities of matched eligible students only.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> comparison(String surveyId, Map<String, Object> report) {
        SmartSurveyUpload u = uploads.findById(surveyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Questionnaire upload not found."));
        Map<String, Map<String, Object>> performance = new LinkedHashMap<>();
        for (Map<String, Object> row : (List<Map<String, Object>>) report.get("students"))
            performance.put(String.valueOf(row.get("studentId")), row);

        int[] sAgree = new int[4], sValid = new int[4], aCorrect = new int[4], aTotal = new int[4];
        int[] measuredStudents = new int[4];
        List<Map<String, Object>> students = new ArrayList<>();
        int notEligible = 0;
        for (SmartSurveyUpload.Row row : u.getRows()) {
            if (!row.isCounted() || row.getStudentId() == null) continue;
            Map<String, Object> perf = performance.get(row.getStudentId());
            if (perf == null) { notEligible++; continue; } // matched account outside the selected cohort
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("studentId", row.getStudentId());
            out.put("code", perf.get("code"));
            out.put("name", perf.get("name"));
            out.put("classCode", perf.get("classCode"));
            out.put("rowNumber", row.getRowNumber());
            for (int g = 0; g < 4; g++) {
                int agree = 0, valid = 0;
                for (int q = GOAL_GROUP[g] * 2; q <= GOAL_GROUP[g] * 2 + 1 && q < row.getAnswers().size(); q++) {
                    Boolean a = scale(row.getAnswers().get(q));
                    if (a == null) continue;
                    valid++;
                    if (a) agree++;
                }
                Map<String, Object> metric = (Map<String, Object>) perf.get(GOAL_KEYS[g]);
                boolean measured = "MEASURED".equals(metric.get("status"));
                Double surveyPct = valid > 0 ? SmartGoalsService.round(agree * 100.0 / valid) : null;
                Double actualPct = measured ? (Double) metric.get("percentage") : null;
                Map<String, Object> cell = new LinkedHashMap<>();
                cell.put("surveyAgree", agree);
                cell.put("surveyValid", valid);
                cell.put("surveyPercentage", surveyPct);
                cell.put("surveyStatus", valid > 0 ? "MEASURED" : "INSUFFICIENT_DATA");
                cell.put("actualCorrect", metric.get("correct"));
                cell.put("actualTotal", metric.get("total"));
                cell.put("actualPercentage", actualPct);
                cell.put("actualStatus", measured ? "MEASURED" : metric.get("status"));
                cell.put("difference", surveyPct != null && actualPct != null ? SmartGoalsService.round(actualPct - surveyPct) : null);
                cell.put("meetsTarget", actualPct == null ? null : actualPct >= GOAL_TARGET[g]);
                out.put(GOAL_KEYS[g], cell);
                sAgree[g] += agree; sValid[g] += valid;
                if (measured) {
                    aCorrect[g] += ((Number) metric.get("correct")).intValue();
                    aTotal[g] += ((Number) metric.get("total")).intValue();
                    measuredStudents[g]++;
                }
            }
            students.add(out);
        }
        students.sort(java.util.Comparator.comparing(m -> String.valueOf(m.get("name")).toLowerCase(Locale.ROOT)));

        List<Map<String, Object>> overall = new ArrayList<>();
        for (int g = 0; g < 4; g++) {
            Double s = sValid[g] > 0 ? SmartGoalsService.round(sAgree[g] * 100.0 / sValid[g]) : null;
            Double a = aTotal[g] > 0 ? SmartGoalsService.round(aCorrect[g] * 100.0 / aTotal[g]) : null;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", GOAL_KEYS[g]);
            m.put("target", GOAL_TARGET[g]);
            m.put("surveyAgree", sAgree[g]);
            m.put("surveyValid", sValid[g]);
            m.put("surveyPercentage", s);
            m.put("actualCorrect", aCorrect[g]);
            m.put("actualTotal", aTotal[g]);
            m.put("actualPercentage", a);
            m.put("actualStatus", a == null ? "INSUFFICIENT_DATA" : a >= GOAL_TARGET[g] ? "ACHIEVED" : "NOT_ACHIEVED");
            m.put("difference", s != null && a != null ? SmartGoalsService.round(a - s) : null);
            m.put("studentsWithPerformance", measuredStudents[g]);
            overall.add(m);
        }
        long legacy = u.getRows().stream().filter(r -> "MATCHED".equals(r.getIdentifierStatus()) && r.getStudentId() == null).count();
        Map<String, Object> flags = new LinkedHashMap<>();
        flags.put("unmatchedRows", u.getUnmatchedRows());
        flags.put("noIdentifierRows", u.getNoIdentifierRows());
        flags.put("duplicateRows", u.getDuplicateRows());
        flags.put("matchedNotEligible", notEligible);
        flags.put("legacyMatchedRows", legacy);
        flags.put("unmatchedRowNumbers", u.getRows().stream().filter(r -> "UNMATCHED".equals(r.getIdentifierStatus())).map(SmartSurveyUpload.Row::getRowNumber).limit(50).toList());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("surveyId", u.getId());
        out.put("fileName", u.getFileName());
        out.put("matchedStudents", students.size());
        out.put("students", students);
        out.put("overall", overall);
        out.put("flags", flags);
        return out;
    }

    /** true = Agree/Strongly Agree, false = another valid answer, null = blank or unrecognised. */
    static Boolean scale(String raw) {
        if (raw == null) return null;
        String v = raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (AGREE.contains(v)) return true;
        if (OTHER.contains(v)) return false;
        return null;
    }

    /** RFC 4180 CSV: quoted fields, doubled quotes, commas and line breaks inside quotes. */
    static List<List<String>> parse(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        int i = text.startsWith("﻿") ? 1 : 0;
        for (; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') { field.append('"'); i++; }
                    else quoted = false;
                } else field.append(c);
            } else if (c == '"') quoted = true;
            else if (c == ',') { row.add(field.toString()); field.setLength(0); }
            else if (c == '\r' || c == '\n') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                row.add(field.toString()); field.setLength(0);
                rows.add(row); row = new ArrayList<>();
            } else field.append(c);
        }
        if (quoted) throw bad("The CSV has an unclosed quote.");
        if (field.length() > 0 || !row.isEmpty()) { row.add(field.toString()); rows.add(row); }
        return rows;
    }

    private static String cell(List<String> line, int index) {
        return index < line.size() && line.get(index) != null ? line.get(index).trim() : "";
    }

    /** Strip control characters and markup brackets, then cap the length. */
    private static String clean(String value, int max) {
        if (value == null) return "";
        String v = value.replaceAll("[\\p{Cntrl}<>]", " ").trim();
        return v.length() > max ? v.substring(0, max) : v;
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "";
        }
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}

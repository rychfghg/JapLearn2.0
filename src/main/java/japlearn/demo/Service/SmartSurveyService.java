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

        Set<String> studentEmails = new HashSet<>();
        for (User u : users.findByRoleIgnoreCase("student"))
            if (u.getEmail() != null) studentEmails.add(u.getEmail().trim().toLowerCase(Locale.ROOT));

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
                String email;
                try { email = EmailInput.normalize(cell(line, idCol)); } catch (RuntimeException e) { email = null; }
                if (email != null && !seen.add(email)) { status = "DUPLICATE"; duplicate++; }
                else if (email != null && studentEmails.contains(email)) { status = "MATCHED"; matched++; }
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

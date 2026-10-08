package japlearn.demo.Entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * One uploaded Google Forms export of the Learning Effectiveness questionnaire.
 * Each upload is stored as a new, never-edited record. Respondent email addresses are
 * only used to match and de-duplicate at upload time; they are not stored.
 */
@Document(collection = "smart_survey_uploads")
public class SmartSurveyUpload {
    @Id private String id;
    private String fileName;
    private String sha256;
    private Instant uploadedAt = Instant.now();
    private String uploadedBy;
    private List<String> questionHeaders = new ArrayList<>();
    private String identifierHeader;
    private boolean matchedOnly;
    private int totalRows;
    private int countedRows;
    private int matchedRows;
    private int unmatchedRows;
    private int duplicateRows;
    private int noIdentifierRows;
    private List<Row> rows = new ArrayList<>();

    /** One respondent: the 10 mapped answers exactly as submitted, and how the row was treated. */
    public static class Row {
        private int rowNumber;
        private List<String> answers = new ArrayList<>();
        /** MATCHED, UNMATCHED, DUPLICATE or NO_IDENTIFIER */
        private String identifierStatus;
        private boolean counted;

        public int getRowNumber() { return rowNumber; }
        public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }
        public List<String> getAnswers() { return answers; }
        public void setAnswers(List<String> answers) { this.answers = answers; }
        public String getIdentifierStatus() { return identifierStatus; }
        public void setIdentifierStatus(String identifierStatus) { this.identifierStatus = identifierStatus; }
        public boolean isCounted() { return counted; }
        public void setCounted(boolean counted) { this.counted = counted; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
    public List<String> getQuestionHeaders() { return questionHeaders; }
    public void setQuestionHeaders(List<String> questionHeaders) { this.questionHeaders = questionHeaders; }
    public String getIdentifierHeader() { return identifierHeader; }
    public void setIdentifierHeader(String identifierHeader) { this.identifierHeader = identifierHeader; }
    public boolean isMatchedOnly() { return matchedOnly; }
    public void setMatchedOnly(boolean matchedOnly) { this.matchedOnly = matchedOnly; }
    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
    public int getCountedRows() { return countedRows; }
    public void setCountedRows(int countedRows) { this.countedRows = countedRows; }
    public int getMatchedRows() { return matchedRows; }
    public void setMatchedRows(int matchedRows) { this.matchedRows = matchedRows; }
    public int getUnmatchedRows() { return unmatchedRows; }
    public void setUnmatchedRows(int unmatchedRows) { this.unmatchedRows = unmatchedRows; }
    public int getDuplicateRows() { return duplicateRows; }
    public void setDuplicateRows(int duplicateRows) { this.duplicateRows = duplicateRows; }
    public int getNoIdentifierRows() { return noIdentifierRows; }
    public void setNoIdentifierRows(int noIdentifierRows) { this.noIdentifierRows = noIdentifierRows; }
    public List<Row> getRows() { return rows; }
    public void setRows(List<Row> rows) { this.rows = rows; }
}

package japlearn.demo.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Upload safety: decides a file's real type from its first bytes (never trusting the
 * file name or the browser-supplied content type), allows only known media formats,
 * cleans file names, and strips anything executable out of PDFs before they are stored.
 */
public final class FileSafety {
    private FileSafety() {}

    public static final Set<String> IMAGES = Set.of("image/png", "image/jpeg", "image/gif", "image/webp");
    public static final Set<String> AUDIO = Set.of("audio/mpeg", "audio/wav", "audio/mp4", "audio/ogg", "audio/webm");
    public static final Set<String> VIDEO = Set.of("video/mp4", "video/webm");

    /** Returns the real media type from the file signature, or null if it is not a supported format. */
    public static String sniff(byte[] b) {
        if (b == null || b.length < 12) return null;
        if (b[0] == (byte) 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "image/png";
        if (b[0] == (byte) 0xFF && b[1] == (byte) 0xD8 && b[2] == (byte) 0xFF) return "image/jpeg";
        if (b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') return "image/gif";
        if (ascii(b, 0, "RIFF") && ascii(b, 8, "WEBP")) return "image/webp";
        if (ascii(b, 0, "RIFF") && ascii(b, 8, "WAVE")) return "audio/wav";
        if (ascii(b, 0, "ID3") || (b[0] == (byte) 0xFF && (b[1] & 0xE0) == 0xE0)) return "audio/mpeg";
        if (ascii(b, 0, "OggS")) return "audio/ogg";
        if (b[0] == 0x1A && b[1] == 0x45 && b[2] == (byte) 0xDF && b[3] == (byte) 0xA3) return "video/webm";
        if (ascii(b, 4, "ftyp")) return ascii(b, 8, "M4A ") ? "audio/mp4" : "video/mp4";
        if (ascii(b, 0, "%PDF-")) return "application/pdf";
        return null;
    }

    /** Checks the bytes are one of the allowed types and returns the real content type to store. */
    public static String requireType(byte[] bytes, Set<String> allowed, String what) {
        String type = sniff(bytes);
        if (type == null || !allowed.contains(type)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported " + what + " file type");
        }
        return type;
    }

    /** Keeps only safe characters in a stored file name (no paths, scripts or control characters). */
    public static String safeName(String original, String fallback) {
        String name = original == null ? "" : original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        if (name.isEmpty() || name.startsWith(".")) name = fallback;
        return name.length() > 80 ? name.substring(name.length() - 80) : name;
    }

    /**
     * Parses the PDF and removes active content: document/page auto-run actions,
     * embedded JavaScript, embedded (attached) files, interactive forms, and any
     * annotation that carries an action. Returns the cleaned PDF bytes.
     */
    public static byte[] sanitizePdf(byte[] bytes) {
        if (sniff(bytes) == null || !"application/pdf".equals(sniff(bytes))) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Upload a PDF file");
        }
        try (PDDocument doc = Loader.loadPDF(bytes); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (doc.isEncrypted()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password-protected PDFs are not supported");
            if (doc.getNumberOfPages() < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The PDF has no pages");
            PDDocumentCatalog catalog = doc.getDocumentCatalog();
            catalog.getCOSObject().removeItem(COSName.OPEN_ACTION);
            catalog.getCOSObject().removeItem(COSName.AA);
            catalog.getCOSObject().removeItem(COSName.ACRO_FORM);
            if (catalog.getNames() != null) {
                catalog.getNames().getCOSObject().removeItem(COSName.JAVA_SCRIPT);
                catalog.getNames().getCOSObject().removeItem(COSName.EMBEDDED_FILES);
            }
            for (PDPage page : doc.getPages()) {
                page.getCOSObject().removeItem(COSName.AA);
                java.util.List<PDAnnotation> keep = new java.util.ArrayList<>();
                for (PDAnnotation a : page.getAnnotations()) {
                    boolean active = a.getCOSObject().containsKey(COSName.A) || a.getCOSObject().containsKey(COSName.AA)
                            || "FileAttachment".equals(a.getSubtype()) || "Widget".equals(a.getSubtype()) || "Screen".equals(a.getSubtype())
                            || "Movie".equals(a.getSubtype()) || "Sound".equals(a.getSubtype()) || "RichMedia".equals(a.getSubtype());
                    if (!active) keep.add(a);
                }
                page.setAnnotations(keep);
            }
            doc.save(out);
            return out.toByteArray();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The PDF could not be read");
        }
    }

    /** Trims text and enforces a maximum length; returns null for blank input. */
    public static String limit(String value, int max, String field) {
        if (value == null) return null;
        String v = value.strip();
        if (v.length() > max) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is too long (max " + max + " characters)");
        return v;
    }

    private static boolean ascii(byte[] b, int at, String s) {
        if (b.length < at + s.length()) return false;
        for (int i = 0; i < s.length(); i++) if (b[at + i] != (byte) s.charAt(i)) return false;
        return true;
    }
}

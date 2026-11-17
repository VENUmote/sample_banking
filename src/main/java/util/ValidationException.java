package util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Checked exception thrown when user input fails server-side validation.
 * Carries an optional map of field name -> error message so the frontend can
 * highlight each invalid field.
 */
public class ValidationException extends Exception {

    private static final long serialVersionUID = 1L;

    private final Map<String, String> fieldErrors;

    public ValidationException(String message) {
        this(message, Collections.emptyMap());
    }

    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    /** @return field-level errors (may be empty, never null) */
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}

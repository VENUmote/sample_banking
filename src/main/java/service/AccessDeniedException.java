package service;

/** Thrown when a user tries to operate on an account they do not own (HTTP 403). */
public class AccessDeniedException extends Exception {

    private static final long serialVersionUID = 1L;

    public AccessDeniedException(String message) {
        super(message);
    }
}

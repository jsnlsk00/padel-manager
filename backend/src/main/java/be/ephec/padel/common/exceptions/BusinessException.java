package be.ephec.padel.common.exceptions;

/** Violation d'une regle metier : renvoyee au client en 409 Conflict. */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}

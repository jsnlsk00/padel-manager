package be.ephec.padel.common.exceptions;

/** Operation interdite au role courant : renvoyee au client en 403 Forbidden. */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}

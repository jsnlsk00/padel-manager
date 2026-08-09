package be.ephec.padel.common.exceptions;

/** Ressource inexistante : renvoyee au client en 404 Not Found. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " introuvable : " + id);
    }
}

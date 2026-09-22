package com.servicioagua.common.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String recurso, Object id) {
        super("No se encontró " + recurso + " con identificador: " + id);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
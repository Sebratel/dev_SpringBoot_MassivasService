package br.com.sebratel.massivas.exceptions;

public class InvalidMassiveRequestException extends RuntimeException {
    public InvalidMassiveRequestException(String message) {
        super(message);
    }
}

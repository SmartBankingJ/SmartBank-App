package ma.bankdirect.exception;

public class InsufficientFundsException extends BusinessException {
    public InsufficientFundsException() {
        super("Solde insuffisant");
    }
}

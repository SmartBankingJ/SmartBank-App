package ma.bankdirect;

import java.math.BigDecimal;

import ma.bankdirect.config.AppConfig;
import ma.bankdirect.model.enums.AccountStatus;
import ma.bankdirect.model.enums.AccountType;
import ma.bankdirect.model.enums.Direction;
import ma.bankdirect.model.enums.TransactionStatus;
//import ma.bankdirect.model.enums.TransactionType;
import ma.bankdirect.util.Money;

public class TestApp {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("       BMCE DIRECT - DOMAIN & MONEY TESTS         ");
        System.out.println("==================================================");

        // 1. Test parsing inputs
        String[] testInputs = { "500", "1 250,50", "5555250.50", "99.9" };
        for (String input : testInputs) {
            BigDecimal parsed = Money.of(input);
            System.out.printf("Input: %-10s -> Parsed: %-10s -> Formatted: %s%n",
                    "\"" + input + "\"", parsed, Money.format(parsed));
        }

        // 2. Test positive validation
        BigDecimal positive = Money.of("10.00");
        BigDecimal zero = Money.of("0.00");
        BigDecimal negative = Money.of("-50.00");

        System.out.println("\n--- Testing Money.isPositive ---");
        System.out.println("isPositive(10.00)  : " + Money.isPositive(positive) + " (Expected: true)");
        System.out.println("isPositive(0.00)   : " + Money.isPositive(zero)     + " (Expected: false)");
        System.out.println("isPositive(-50.00) : " + Money.isPositive(negative) + " (Expected: false)");
        System.out.println("isPositive(null)   : " + Money.isPositive(null)     + " (Expected: false)");

        // 3. Test Invalid Inputs (Exceptions)
        System.out.println("\n--- Testing Invalid Inputs ---");
        try {
            Money.of("   ");
            System.err.println("FAILED: Should have thrown IllegalArgumentException for blank input");
        } catch (IllegalArgumentException e) {
            System.out.println("PASSED: Caught expected exception: " + e.getMessage());
        }

        // 4. Test Enums
        System.out.println("\n--- Testing Domain Enums ---");
        System.out.println("Account types available:");
        for (AccountType t : AccountType.values()) {
            System.out.println(" - " + t.name());
        }

        System.out.println("Account status check: " + AccountStatus.ACTIVE);
        System.out.println("Transaction direction: " + Direction.DEBIT + " / " + Direction.CREDIT);
        System.out.println("Transaction status: " + TransactionStatus.COMPLETED);

        // 5. Test AppConfig
        System.out.println("\n--- Testing AppConfig ---");
        System.out.println("App Name: " + ma.bankdirect.config.AppConfig.get("app.name"));
        System.out.println("App Currency: " + ma.bankdirect.config.AppConfig.get("app.currency"));
        System.out.println("DB URL: " + ma.bankdirect.config.AppConfig.get("db.url"));

        System.out.println("\n==================================================");
        System.out.println("       ALL INITIAL CHECKS COMPLETED               ");
        System.out.println("==================================================");
    }
}

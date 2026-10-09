package ma.bankdirect.util;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public final class Money {
    private Money(){
    }
    
    public static final int SCALE = 2;
    //methods used
    public static BigDecimal of(String text){
        if(text == null || text.isBlank()){
            throw new IllegalArgumentException("Amount cannot be empty");
        }
        String clean = text.trim().replace(" ", "").replace(',', '.');
        return new BigDecimal(clean).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.signum() > 0;
    }
    public static String format(BigDecimal amount){
        // Locale.FRANCE
        if(amount == null) return "0,00 MAD";
        return NumberFormat.getNumberInstance(Locale.FRANCE).format(amount)+" MAD";
    }


}

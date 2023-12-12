package pdc.node;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

import org.apache.commons.lang3.RandomStringUtils;

public class DataGenerator {

    private Random random = new Random();

    public String generateTextData(int length) {
        boolean useLetters = true;
        boolean useNumbers = false;
        String generatedString = RandomStringUtils.random(length, useLetters, useNumbers);

        return generatedString;
    }

    public String generateNumberData() {
        BigDecimal min = new BigDecimal(-100000);
        BigDecimal range = new BigDecimal(100000).subtract(min);

        BigDecimal randomBigDecimal = min.add(new BigDecimal(range.doubleValue() * random.nextDouble()));
        randomBigDecimal = randomBigDecimal.setScale(4, RoundingMode.HALF_UP);
        return randomBigDecimal.toString();
    }
}

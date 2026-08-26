package com.recruitcrm;

import com.recruitcrm.patterns.decorator.compensation.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DECORATOR PATTERN tests.
 *
 * The most important test in this class is orderOfDecoratorsChangesTheTotal():
 * it proves the chain is genuinely composing rather than just summing
 * independent values.
 */
class DecoratorTest {

    private static final double DELTA = 0.001;   // floating-point tolerance

    @Test
    @DisplayName("An undecorated base salary returns exactly the base amount")
    void baseSalaryAlone() {
        Compensation c = new BaseSalary("Backend Engineer", 50_000);

        assertEquals(50_000, c.getMonthlyAmount(), DELTA);
        assertEquals(1, c.getBreakdown().size(), "base salary is a single line item");
        assertTrue(c.getDescription().contains("Backend Engineer"));
    }

    @Test
    @DisplayName("A fixed decorator adds its exact amount")
    void fixedDecoratorAddsFlatAmount() {
        Compensation c = new TransportAllowanceDecorator(
                new BaseSalary("Backend Engineer", 50_000), 5_000);

        assertEquals(55_000, c.getMonthlyAmount(), DELTA);
        assertEquals(2, c.getBreakdown().size());
    }

    @Test
    @DisplayName("A percentage decorator calculates from what it wraps")
    void percentageDecoratorUsesWrappedAmount() {
        // 40% of 50,000 = 20,000
        Compensation c = new HousingAllowanceDecorator(
                new BaseSalary("Backend Engineer", 50_000), 40);

        assertEquals(70_000, c.getMonthlyAmount(), DELTA);
    }

    @Test
    @DisplayName("Decorators stack, each adding a line to the breakdown")
    void decoratorsStack() {
        Compensation c =
            new MeritBonusDecorator(
                new TransportAllowanceDecorator(
                    new HousingAllowanceDecorator(
                        new BaseSalary("Backend Engineer", 50_000), 40),
                    5_000),
                "Exceptional technical assessment", 8_000);

        // 50,000 + 20,000 + 5,000 + 8,000
        assertEquals(83_000, c.getMonthlyAmount(), DELTA);
        assertEquals(4, c.getBreakdown().size(), "one line per component");
    }

    @Test
    @DisplayName("THE KEY TEST: decorator order changes the computed total")
    void orderOfDecoratorsChangesTheTotal() {
        // Housing first: 50,000 -> +20,000 (40% of 50,000) -> +5,000 flat
        Compensation housingFirst = new TransportAllowanceDecorator(
                new HousingAllowanceDecorator(
                        new BaseSalary("Backend Engineer", 50_000), 40),
                5_000);

        // Transport first: 50,000 -> +5,000 flat -> +22,000 (40% of 55,000)
        Compensation transportFirst = new HousingAllowanceDecorator(
                new TransportAllowanceDecorator(
                        new BaseSalary("Backend Engineer", 50_000), 5_000),
                40);

        assertEquals(75_000, housingFirst.getMonthlyAmount(), DELTA);
        assertEquals(77_000, transportFirst.getMonthlyAmount(), DELTA);

        assertNotEquals(housingFirst.getMonthlyAmount(),
                        transportFirst.getMonthlyAmount(),
                        "percentage decorators compute from what is beneath them, "
                      + "so the chain order genuinely matters");
    }

    @Test
    @DisplayName("A merit bonus carries its justification into the breakdown")
    void meritBonusCarriesItsReason() {
        Compensation c = new MeritBonusDecorator(
                new BaseSalary("Backend Engineer", 50_000),
                "Strong prior experience", 4_500);

        assertTrue(c.getDescription().contains("Strong prior experience"));
        assertTrue(c.getBreakdown().get(1).contains("Strong prior experience"));
    }

    @Test
    @DisplayName("The performance bonus is driven by the evaluation score")
    void performanceBonusUsesEvaluationScore() {
        // score 78 -> 7.8% of 50,000 = 3,900
        Compensation c = new PerformanceBonusDecorator(
                new BaseSalary("Backend Engineer", 50_000), 78);

        assertEquals(53_900, c.getMonthlyAmount(), DELTA);
    }

    @Test
    @DisplayName("The calculator builds a chain from selected entitlement keys")
    void calculatorBuildsChainFromKeys() {
        Compensation c = CompensationCalculator.build(
                "Backend Engineer", 50_000, "HOUSING,TRANSPORT", 0);

        assertEquals(75_000, c.getMonthlyAmount(), DELTA);
    }

    @Test
    @DisplayName("No entitlements selected leaves the base salary undecorated")
    void noEntitlementsMeansBaseOnly() {
        Compensation c = CompensationCalculator.build("Backend Engineer", 50_000, "", 0);

        assertEquals(50_000, c.getMonthlyAmount(), DELTA);
        assertEquals(1, c.getBreakdown().size());
    }

    @Test
    @DisplayName("The calculator parses discretionary bonuses with reasons")
    void calculatorParsesMeritBonuses() {
        Compensation c = CompensationCalculator.build(
                "Backend Engineer", 50_000,
                "BONUS:Exceptional technical assessment:8000", 0);

        assertEquals(58_000, c.getMonthlyAmount(), DELTA);
        assertTrue(c.getDescription().contains("Exceptional technical assessment"));
    }

    @Test
    @DisplayName("An unparseable bonus amount is skipped rather than crashing the offer")
    void malformedBonusIsIgnored() {
        Compensation c = CompensationCalculator.build(
                "Backend Engineer", 50_000, "BONUS:Some reason:notanumber", 0);

        assertEquals(50_000, c.getMonthlyAmount(), DELTA);
    }
}

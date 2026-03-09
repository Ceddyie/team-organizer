package de.ceddyie.organizerbackend.util;

import org.junit.jupiter.api.RepeatedTest;

import static org.junit.jupiter.api.Assertions.*;

class InviteCodeGeneratorTest {

    private static final String ALLOWED = "[A-Z2-9]{8}";

    @RepeatedTest(25)
    void generate_returnsEightCharacterCode_withAllowedAlphabet() {
        String code = InviteCodeGenerator.generate();

        assertNotNull(code);
        assertEquals(8, code.length());
        assertTrue(code.matches(ALLOWED));
    }
}

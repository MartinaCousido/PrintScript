package org.printscript.semantic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class TypeTest {

    @Test
    void translatesTheWrittenAnnotationToTheSemanticType() {
        assertEquals(Optional.of(Type.NUMBER), Type.fromWrittenName("number"));
        assertEquals(Optional.of(Type.STRING), Type.fromWrittenName("string"));
    }

    @Test
    void anAnnotationThatIsNotATypeHasNoTranslation() {
        assertEquals(Optional.empty(), Type.fromWrittenName("boolean"));
        assertEquals(Optional.empty(), Type.fromWrittenName("Number"));
        assertEquals(Optional.empty(), Type.fromWrittenName(""));
    }

    @Test
    void everyTypeKnowsHowItIsWritten() {
        assertEquals("number", Type.NUMBER.writtenName());
        assertEquals("string", Type.STRING.writtenName());
    }
}

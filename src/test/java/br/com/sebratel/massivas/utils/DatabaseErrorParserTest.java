package br.com.sebratel.massivas.utils;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseErrorParserTest {

    @Test
    void shouldParseNullColumnError() {
        String msg = "Column 'created_by' cannot be null";
        Map<String, Object> result = DatabaseErrorParser.parse(msg);
        
        assertNotNull(result);
        assertTrue(result.containsKey("database"));
        Map<String, String> dbErrors = (Map<String, String>) result.get("database");
        assertEquals("cannot be null", dbErrors.get("created_by"));
    }

    @Test
    void shouldParseDuplicateEntryError() {
        String msg = "Duplicate entry '123' for key 'PRIMARY'";
        Map<String, Object> result = DatabaseErrorParser.parse(msg);
        
        assertNotNull(result);
        Map<String, String> dbErrors = (Map<String, String>) result.get("database");
        assertEquals("duplicate entry: 123", dbErrors.get("PRIMARY"));
    }

    @Test
    void shouldParseDataTooLongError() {
        String msg = "Data too long for column 'user_pppoe'";
        Map<String, Object> result = DatabaseErrorParser.parse(msg);
        
        assertNotNull(result);
        Map<String, String> dbErrors = (Map<String, String>) result.get("database");
        assertEquals("data too long", dbErrors.get("user_pppoe"));
    }

    @Test
    void shouldParseForeignKeyError() {
        String msg = "Cannot add or update a child row: a foreign key constraint fails (`db`.`table`, CONSTRAINT `fk_name` FOREIGN KEY ('protocol_id') REFERENCES `other` (`id`))";
        Map<String, Object> result = DatabaseErrorParser.parse(msg);
        
        assertNotNull(result);
        Map<String, String> dbErrors = (Map<String, String>) result.get("database");
        assertEquals("foreign key constraint violation", dbErrors.get("protocol_id"));
    }

    @Test
    void shouldReturnNullForUnknownError() {
        String msg = "Some random database error";
        Map<String, Object> result = DatabaseErrorParser.parse(msg);
        assertNull(result);
    }

    @Test
    void shouldReturnNullForEmptyMessage() {
        assertNull(DatabaseErrorParser.parse(""));
        assertNull(DatabaseErrorParser.parse(null));
    }
}

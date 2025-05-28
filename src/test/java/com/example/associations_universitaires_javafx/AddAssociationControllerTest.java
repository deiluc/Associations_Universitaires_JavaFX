package com.example.associations_universitaires_javafx;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.Properties;
import javafx.scene.control.TextField;

import static org.junit.jupiter.api.Assertions.*;

public class AddAssociationControllerTest {

    private TestAddAssociationController controller;
    private PreparedStatement mockPreparedStatement;
    private boolean wasExecuted;

    // Subclass to override database interaction
    private static class TestAddAssociationController extends AddAssociationController {
        private PreparedStatement mockStmt;
        private boolean wasExecuted;

        public void setMockPreparedStatement(PreparedStatement stmt, boolean[] wasExecutedFlag) {
            this.mockStmt = stmt;
            this.wasExecuted = wasExecutedFlag[0];
        }

        @Override
        protected void saveAssociationToDatabase(String name, String description, String abbreviation, String email, String phone) throws SQLException {
            mockStmt.setString(1, name);
            mockStmt.setString(2, description);
            mockStmt.setString(3, abbreviation);
            mockStmt.setString(4, email);
            mockStmt.setString(5, phone);
            mockStmt.executeUpdate();
            wasExecuted = true;
        }

        public boolean wasExecuted() {
            return wasExecuted;
        }
    }

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        controller = new TestAddAssociationController();

        wasExecuted = false;
        mockPreparedStatement = new PreparedStatement() {
            @Override
            public int executeUpdate() throws SQLException {
                wasExecuted = true;
                return 1;
            }
            @Override public void setNull(int parameterIndex, int sqlType) throws SQLException {}
            @Override public ResultSet executeQuery() { return null; }
            @Override public boolean execute() { return false; }
            @Override public void setInt(int parameterIndex, int x) {}
            @Override public void setString(int parameterIndex, String x) {}
            @Override public ParameterMetaData getParameterMetaData() { return null; }
            @Override public void close() {}
            @Override public void setLong(int parameterIndex, long x) {}
            @Override public void setFloat(int parameterIndex, float x) {}
            @Override public void setDouble(int parameterIndex, double x) {}
            @Override public void setBigDecimal(int parameterIndex, java.math.BigDecimal x) {}
            @Override public void setBytes(int parameterIndex, byte[] x) {}
            @Override public void setDate(int parameterIndex, Date x) {}
            @Override public void setTime(int parameterIndex, Time x) {}
            @Override public void setTimestamp(int parameterIndex, Timestamp x) {}
            @Override public void setAsciiStream(int parameterIndex, java.io.InputStream x, int length) {}
            @Override public void setUnicodeStream(int parameterIndex, java.io.InputStream x, int length) {}
            @Override public void setBinaryStream(int parameterIndex, java.io.InputStream x, int length) {}
            @Override public void clearParameters() {}
            @Override public void setObject(int parameterIndex, Object x, int targetSqlType) {}
            @Override public void setObject(int parameterIndex, Object x) {}
            @Override public void addBatch() {}
            @Override public void setCharacterStream(int parameterIndex, java.io.Reader reader, int length) {}
            @Override public void setRef(int parameterIndex, Ref x) {}
            @Override public void setBlob(int parameterIndex, Blob x) {}
            @Override public void setClob(int parameterIndex, Clob x) {}
            @Override public void setArray(int parameterIndex, Array x) {}
            @Override public ResultSetMetaData getMetaData() { return null; }
            @Override public void setDate(int parameterIndex, Date x, java.util.Calendar cal) {}
            @Override public void setTime(int parameterIndex, Time x, java.util.Calendar cal) {}
            @Override public void setTimestamp(int parameterIndex, Timestamp x, java.util.Calendar cal) {}
            @Override public void setNull(int parameterIndex, int sqlType, String typeName) {}
            @Override public void setURL(int parameterIndex, java.net.URL x) {}
            @Override public void setRowId(int parameterIndex, RowId x) {}
            @Override public void setNString(int parameterIndex, String value) {}
            @Override public void setNCharacterStream(int parameterIndex, java.io.Reader value, long length) {}
            @Override public void setNClob(int parameterIndex, NClob value) {}
            @Override public void setClob(int parameterIndex, java.io.Reader reader, long length) {}
            @Override public void setBlob(int parameterIndex, java.io.InputStream inputStream, long length) {}
            @Override public void setNClob(int parameterIndex, java.io.Reader reader, long length) {}
            @Override public void setSQLXML(int parameterIndex, SQLXML xmlObject) {}
            @Override public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) {}
            @Override public void setAsciiStream(int parameterIndex, java.io.InputStream x, long length) {}
            @Override public void setBinaryStream(int parameterIndex, java.io.InputStream x, long length) {}
            @Override public void setCharacterStream(int parameterIndex, java.io.Reader reader, long length) {}
            @Override public void setAsciiStream(int parameterIndex, java.io.InputStream x) {}
            @Override public void setBinaryStream(int parameterIndex, java.io.InputStream x) {}
            @Override public void setCharacterStream(int parameterIndex, java.io.Reader reader) {}
            @Override public void setNCharacterStream(int parameterIndex, java.io.Reader value) {}
            @Override public void setClob(int parameterIndex, java.io.Reader reader) {}
            @Override public void setBlob(int parameterIndex, java.io.InputStream inputStream) {}
            @Override public void setNClob(int parameterIndex, java.io.Reader reader) {}
            @Override public ResultSet executeQuery(String sql) { return null; }
            @Override public int executeUpdate(String sql) { return 0; }
            @Override public boolean execute(String sql) { return false; }
            @Override public int[] executeBatch() { return new int[0]; }
            @Override public void addBatch(String sql) {}
            @Override public void clearBatch() {}
            @Override public void setBoolean(int parameterIndex, boolean x) {}
            @Override public void setByte(int parameterIndex, byte x) {}
            @Override public void setShort(int parameterIndex, short x) {}
            @Override public void setFloat(int parameterIndex, float x, java.util.Calendar cal) {}
            @Override public void setDouble(int parameterIndex, double x, java.util.Calendar cal) {}
            @Override public void setBigDecimal(int parameterIndex, java.math.BigDecimal x, java.util.Calendar cal) {}
            @Override public Connection getConnection() { return null; }
            @Override public SQLWarning getWarnings() { return null; }
            @Override public void clearWarnings() {}
            @Override public void setCursorName(String name) {}
            @Override public ResultSet getResultSet() { return null; }
            @Override public int getUpdateCount() { return 0; }
            @Override public boolean getMoreResults() { return false; }
            @Override public void setFetchDirection(int direction) {}
            @Override public int getFetchDirection() { return 0; }
            @Override public void setFetchSize(int rows) {}
            @Override public int getFetchSize() { return 0; }
            @Override public int getResultSetConcurrency() { return 0; }
            @Override public int getResultSetType() { return 0; }
            @Override public boolean getMoreResults(int current) { return false; }
            @Override public ResultSet getGeneratedKeys() { return null; }
            @Override public int getResultSetHoldability() { return 0; }
            @Override public boolean isClosed() { return false; }
            @Override public void setPoolable(boolean poolable) {}
            @Override public boolean isPoolable() { return false; }
            @Override public void closeOnCompletion() {}
            @Override public boolean isCloseOnCompletion() { return false; }
            @Override public <T> T unwrap(Class<T> iface) { return null; }
            @Override public boolean isWrapperFor(Class<?> iface) { return false; }
        };

        controller.setMockPreparedStatement(mockPreparedStatement, new boolean[]{wasExecuted});

        setField("associationNameField", new TextField());
        setField("descriptionField", new TextField());
        setField("abbreviationField", new TextField());
        setField("emailField", new TextField());
        setField("phoneField", new TextField());
    }

    private void setField(String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = AddAssociationController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(controller, value);
    }

    private TextField getField(String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = AddAssociationController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (TextField) field.get(controller);
    }

    @Test
    void testHandleCreateAssociation_Success() throws Exception {
        getField("associationNameField").setText("Test Association");
        getField("descriptionField").setText("Test Description");
        getField("abbreviationField").setText("TA");
        getField("emailField").setText("test@email.com");
        getField("phoneField").setText("123456789");

        Method handleCreateAssociation = AddAssociationController.class.getDeclaredMethod("handleCreateAssociation");
        handleCreateAssociation.setAccessible(true);
        handleCreateAssociation.invoke(controller);

        assertTrue(controller.wasExecuted(), "The executeUpdate method should have been called");
    }

    @Test
    void testHandleCreateAssociation_Failure_EmptyName() throws Exception {
        getField("associationNameField").setText("");
        getField("descriptionField").setText("Test Description");

        Method handleCreateAssociation = AddAssociationController.class.getDeclaredMethod("handleCreateAssociation");
        handleCreateAssociation.setAccessible(true);

        assertThrows(IllegalArgumentException.class, () -> {
            handleCreateAssociation.invoke(controller);
        }, "Should throw IllegalArgumentException for empty name");
    }
}
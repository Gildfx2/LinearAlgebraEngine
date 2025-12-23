package memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SharedMatrixTest {


    @Test
    void loadRowMajor() {
        double[][] data = {{1, 3}, {2, 4.5}, {5.1, 9}}; // 3x2 matrix
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadRowMajor(data);
        assertEquals(3, matrix.length(), "Should have 3 vectors (rows)");
        assertEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation());

        // Checking internal values
        for (int r = 0; r < data.length; r++) {
            for (int c = 0; c < data[0].length; c++) {
                assertEquals(data[r][c], matrix.get(r).get(c),
                        "Mismatch at row " + r + " col " + c);
            }
        }
    }

    @Test
    void loadColumnMajor() {
        double[][] data = {{1, 3}, {2, 4.5}, {5.1, 9}}; // 3x2 matrix
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(data);

        assertEquals(2, matrix.length(), "Should have 2 vectors (columns)");
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());

        // Checking internal values (vectors are columns)
        for (int c = 0; c < data[0].length; c++) {
            for (int r = 0; r < data.length; r++) {
                assertEquals(data[r][c], matrix.get(c).get(r),
                        "Mismatch at col " + c + " row " + r);
            }
        }
    }

    @Test
    void testReadRowMajor_FromColumnMajorLoad() {
        double[][] original = {
                {1.0, 2.0},
                {3.0, 4.0},
                {5.0, 6.0}
        };

        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(original);

        double[][] result = matrix.readRowMajor();

        assertEquals(original.length, result.length, "Rows count mismatch");
        assertEquals(original[0].length, result[0].length, "Cols count mismatch");

        for (int r = 0; r < original.length; r++) {
            for (int c = 0; c < original[0].length; c++) {
                assertEquals(original[r][c], result[r][c],
                        "Mismatch at [" + r + "][" + c + "]");
            }
        }
    }

}
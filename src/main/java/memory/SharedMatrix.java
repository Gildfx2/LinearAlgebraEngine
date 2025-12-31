package memory;

public class SharedMatrix {

    private volatile SharedVector[] vectors = {}; // underlying vectors

    public SharedMatrix() {
        // TODO: initialize empty matrix
        vectors = new SharedVector[0];
    }

    public SharedMatrix(double[][] matrix) {
        // TODO: construct matrix as row-major SharedVectors
        vectors = new SharedVector[matrix.length];
        for(int i=0;i<matrix.length;i++){
            vectors[i] = new SharedVector(matrix[i],VectorOrientation.ROW_MAJOR);
        }
    }

    public void loadRowMajor(double[][] matrix) {
        // TODO: replace internal data with new row-major matrix
        SharedVector[] newVectors = new SharedVector[matrix.length];
        for(int i=0 ; i<matrix.length ; i++){
            SharedVector vector = new SharedVector(matrix[i], VectorOrientation.ROW_MAJOR);
            newVectors[i] = vector;
        }
        this.vectors = newVectors;
    }

    // we load a column matrix by iterating over all the columns of the matrix and getting a vector of the value of each row and saving the vector as column vectors
    public void loadColumnMajor(double[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;

        SharedVector[] newVectors = new SharedVector[cols];

        for (int col = 0; col < cols; col++) {
            double[] columnData = new double[rows];
            for (int row = 0; row < rows; row++) {
                columnData[row] = matrix[row][col];
            }
            newVectors[col] = new SharedVector(columnData, VectorOrientation.COLUMN_MAJOR);
        }

        this.vectors = newVectors;
    }
    // if its a matrix by rows we simply get vectors[i][j] else we do get [j][i]
    public double[][] readRowMajor() {
        acquireAllVectorReadLocks(vectors);

        try {
            double[][] result;

            if (getOrientation() == VectorOrientation.COLUMN_MAJOR) {
                int numCols = vectors.length;
                int numRows = vectors[0].length();

                result = new double[numRows][numCols];

                for (int i = 0; i < numCols; i++) {
                    for (int j = 0; j < numRows; j++) {
                        result[j][i] = vectors[i].get(j);
                    }
                }
            } else {
                result = new double[vectors.length][vectors[0].length()];
                for (int i = 0; i < vectors.length; i++) {
                    for (int j = 0; j < vectors[i].length(); j++) {
                        result[i][j] = vectors[i].get(j);
                    }
                }
            }
            return result;
        } finally {
            releaseAllVectorReadLocks(vectors);
        }
    }

    public SharedVector get(int index) {
        // TODO: return vector at index
        return vectors[index];
    }

    public int length() {
        // TODO: return number of stored vectors
        return vectors.length;
    }

    public VectorOrientation getOrientation() {
        // TODO: return orientation
        return vectors[0].getOrientation();
    }

    private void acquireAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: acquire read lock for each vector
        for(int i=0;i<length();i++){
            vectors[i].readLock();
        }
    }

    private void releaseAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: release read locks
        for(int i=0;i<length();i++){
            vectors[i].readUnlock();
        }
    }

    private void acquireAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: acquire write lock for each vector
        for(int i=0;i<length();i++){
            vectors[i].writeLock();
        }
    }

    private void releaseAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: release write locks
        for(int i=0;i<length();i++){
            vectors[i].writeUnlock();
        }
    }
}

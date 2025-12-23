package memory;

import java.util.concurrent.locks.ReadWriteLock;

public class SharedVector {

    private double[] vector;
    private VectorOrientation orientation;
    private ReadWriteLock lock = new java.util.concurrent.locks.ReentrantReadWriteLock();

    public SharedVector(double[] vector, VectorOrientation orientation) {
        // TODO: store vector data and its orientation
        this.orientation = orientation;
        this.vector = new double[vector.length];
        for(int i=0;i<vector.length;i++){
            this.vector[i] = vector[i];
        }
    }

    public double get(int index) {
        // TODO: return element at index (read-locked)
        return vector[index];
    }

    public int length() {
        // TODO: return vector length
        return vector.length;
    }

    public VectorOrientation getOrientation() {
        // TODO: return vector orientation
        return orientation;
    }

    public void writeLock() {
        lock.writeLock().lock();
    }

    public void writeUnlock() {
        lock.writeLock().unlock();
    }

    public void readLock() {
        lock.readLock().lock();
    }

    public void readUnlock() {
        lock.readLock().unlock();
    }

    public void transpose() {
        orientation = orientation == VectorOrientation.ROW_MAJOR ? VectorOrientation.COLUMN_MAJOR : VectorOrientation.ROW_MAJOR;
    }

    public void add(SharedVector other) {
        // TODO: add two vectors
        for(int i=0;i<vector.length;i++){
            vector[i]=vector[i]+other.get(i);
        }
    }

    public void negate() {
        // TODO: negate vector
        for(int i=0;i<vector.length;i++){
            vector[i] = -vector[i];
        }
    }

    public double dot(SharedVector other) {
        // TODO: compute dot product (row · column)
        double result = 0;
        for(int i=0;i<vector.length;i++){
           result += vector[i]*other.get(i);
        }
        return result;
    }


    public void vecMatMul(SharedMatrix matrix) {
        // TODO: compute row-vector × matrix
        double[] result = new double[matrix.length()];
        for(int i=0;i< matrix.length();i++){
            SharedVector column = matrix.get(i);
            column.readLock();
            try {
                result[i] = this.dot(column);
            } finally {
                column.readUnlock();
            }
        }
        this.vector = result;
    }


}

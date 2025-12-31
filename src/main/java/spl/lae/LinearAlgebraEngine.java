package spl.lae;

import parser.*;
import memory.*;
import scheduling.*;

import java.util.ArrayList;
import java.util.List;

public class LinearAlgebraEngine {

    private SharedMatrix leftMatrix = new SharedMatrix();
    private SharedMatrix rightMatrix = new SharedMatrix();
    private TiredExecutor executor;

    public LinearAlgebraEngine(int numThreads) {
        // TODO: create executor with given thread count
        executor = new TiredExecutor(numThreads);
    }

    public ComputationNode run(ComputationNode computationRoot) {
        // TODO: resolve computation tree step by step until final matrix is produced
        try{
            computationRoot.associativeNesting();
            ComputationNode resolvable = computationRoot.findResolvable();
            while(resolvable!=null){
                loadAndCompute(resolvable);
                resolvable = computationRoot.findResolvable();
            }
            return computationRoot;
        }finally {
            try{
                executor.shutdown();
            }catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }
        }

    }

    public void loadAndCompute(ComputationNode node) {
        // TODO: load operand matrices
        // TODO: create compute tasks & submit tasks to executor
        List<ComputationNode> children = node.getChildren();
        ComputationNodeType type = node.getNodeType();

        // Validate operand counts
        if (type == ComputationNodeType.ADD || type == ComputationNodeType.MULTIPLY) {
            if (children.size() < 2) {
                throw new IllegalArgumentException("Illegal operation: binary operator expects at least two operands");
            }
        } else if (node.getNodeType() == ComputationNodeType.NEGATE || type == ComputationNodeType.TRANSPOSE) {
            if (children.size() != 1) {
                throw new IllegalArgumentException("Illegal operation: unary operator expects exactly one operand");
            }
        } else {
            throw new IllegalArgumentException("Illegal operation: unknown operator '" + node + "'");
        }

        List<Runnable> tasks=null;

        double[][] m1 = children.get(0).getMatrix();

        if(type == ComputationNodeType.ADD){
            double[][] m2 = children.get(1).getMatrix();
            // VALIDATION: Dimensions must be identical
            if (m1.length != m2.length || m1[0].length != m2[0].length) {
                throw new IllegalArgumentException("Illegal operation: dimensions mismatch");
            }
            leftMatrix.loadRowMajor(m1);
            rightMatrix.loadRowMajor(m2);
            tasks = createAddTasks();
        }else if(type == ComputationNodeType.NEGATE){
            leftMatrix.loadRowMajor(m1);
            tasks = createNegateTasks();
        }else if(type == ComputationNodeType.MULTIPLY){
            double[][] m2 = children.get(1).getMatrix();

            // VALIDATION: Cols of A must equal Rows of B
            // Note: m1[0].length is columns of A, m2.length is rows of B
            if (m1[0].length != m2.length) {
                throw new IllegalArgumentException("Illegal operation: dimensions mismatch");
            }
            leftMatrix.loadRowMajor(m1);
            rightMatrix.loadColumnMajor(m2);
            tasks = createMultiplyTasks();
        }else if(node.getNodeType() == ComputationNodeType.TRANSPOSE){
            leftMatrix.loadRowMajor(m1);
            tasks = createTransposeTasks();
        }
        executor.submitAll(tasks);
        node.resolve(leftMatrix.readRowMajor());
    }

    public List<Runnable> createAddTasks() {
        // TODO: return tasks that perform row-wise addition
        List<Runnable> tasks = new ArrayList<>();
        for(int i=0;i<leftMatrix.length();i++) {
            int finalI = i;
            Runnable task = ()->{
                SharedVector leftRow = leftMatrix.get(finalI);
                SharedVector rightRow = rightMatrix.get(finalI);
                rightRow.readLock();
                leftRow.writeLock();
                try {
                    leftRow.add(rightRow);
                } finally {
                    rightRow.readUnlock();
                    leftRow.writeUnlock();
                }
            };
            tasks.add(task);
        }
        return tasks;
    }

    public List<Runnable> createMultiplyTasks() {
        // TODO: return tasks that perform row × matrix multiplication
        List<Runnable> tasks = new ArrayList<>();
        for(int i=0;i<leftMatrix.length();i++) {
            int finalI = i;
            Runnable task = ()->{
                SharedVector leftRow = leftMatrix.get(finalI);
                leftRow.writeLock();
                try {
                    leftRow.vecMatMul(rightMatrix);
                } finally {
                    leftRow.writeUnlock();
                }
            };
            tasks.add(task);
        }
        return tasks;
    }

    public List<Runnable> createNegateTasks() {
        // TODO: return tasks that negate rows
        List<Runnable> tasks = new ArrayList<>();
        for(int i=0;i<leftMatrix.length();i++) {
            int finalI = i;
            Runnable task = ()->{
                SharedVector leftRow = leftMatrix.get(finalI);
                leftRow.writeLock();
                try {
                    leftRow.negate();
                } finally {
                    leftRow.writeUnlock();
                }
            };
            tasks.add(task);
        }
        return tasks;
    }

    public List<Runnable> createTransposeTasks() {
        // TODO: return tasks that transpose rows
        List<Runnable> tasks = new ArrayList<>();
        for(int i=0;i<leftMatrix.length();i++) {
            int finalI = i;
            Runnable task = ()->{
                SharedVector leftRow = leftMatrix.get(finalI);
                leftRow.writeLock();
                try {
                    leftRow.transpose();
                } finally {
                    leftRow.writeUnlock();
                }
            };
            tasks.add(task);
        }
        return tasks;
    }

    public String getWorkerReport() {
        // TODO: return summary of worker activity
        return executor.getWorkerReport();
    }
}

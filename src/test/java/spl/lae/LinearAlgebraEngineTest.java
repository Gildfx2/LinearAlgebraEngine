package spl.lae;

import org.junit.jupiter.api.Test;
import parser.ComputationNode;
import parser.ComputationNodeType;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LinearAlgebraEngineTest {

// --- HELPER METHODS ---

    private ComputationNode createLeafNode(double[][] data) {
        return new ComputationNode(data);
    }

    private ComputationNode createOpNode(ComputationNodeType type, ComputationNode child1, ComputationNode child2) {
        List<ComputationNode> children = new ArrayList<>();
        children.add(child1);
        children.add(child2);
        return new ComputationNode(type, children);
    }

    // For single-child operations like Transpose/Negate
    private ComputationNode createUnaryOpNode(ComputationNodeType type, ComputationNode child) {
        List<ComputationNode> children = new ArrayList<>();
        children.add(child);
        return new ComputationNode(type, children);
    }


    @Test
    void testRun_ComplexTree() {
        // Scenario: (A + B) * C
        // A = [1, 2], B = [3, 4] -> Sum = [4, 6]
        // C = [[2], [2]] (Col vector)
        // Result = (4*2) + (6*2) = 8 + 12 = 20

        LinearAlgebraEngine lae = new LinearAlgebraEngine(4);

        ComputationNode nodeA = createLeafNode(new double[][]{{1.0, 2.0}});
        ComputationNode nodeB = createLeafNode(new double[][]{{3.0, 4.0}});
        ComputationNode nodeC = createLeafNode(new double[][]{{2.0}, {2.0}});

        ComputationNode addNode = createOpNode(ComputationNodeType.ADD, nodeA, nodeB);
        ComputationNode rootNode = createOpNode(ComputationNodeType.MULTIPLY, addNode, nodeC);


        ComputationNode resultNode = lae.run(rootNode);

        // 3. Verify
        double[][] result = resultNode.getMatrix();
        assertEquals(1, result.length); // 1x1 Result
        assertEquals(20.0, result[0][0]);
    }



    @Test
    void testLoadAndCompute_Add() {
        LinearAlgebraEngine lae = new LinearAlgebraEngine(2);

        ComputationNode nodeA = createLeafNode(new double[][]{{1, 2}, {3, 4}});
        ComputationNode nodeB = createLeafNode(new double[][]{{10, 20}, {30, 40}});
        ComputationNode addNode = createOpNode(ComputationNodeType.ADD, nodeA, nodeB);

        lae.loadAndCompute(addNode);

        double[][] res = addNode.getMatrix();
        assertEquals(11.0, res[0][0]);
        assertEquals(44.0, res[1][1]);
    }


    @Test
    void testLoadAndCompute_Negate() {
        LinearAlgebraEngine lae = new LinearAlgebraEngine(2);

        ComputationNode nodeA = createLeafNode(new double[][]{{1, -5}, {-10, 20}});
        ComputationNode negNode = createUnaryOpNode(ComputationNodeType.NEGATE, nodeA);

        lae.loadAndCompute(negNode);

        double[][] res = negNode.getMatrix();
        assertEquals(-1.0, res[0][0]);
        assertEquals(5.0, res[0][1]); // -(-5) = 5
        assertEquals(10.0, res[1][0]);
    }


    @Test
    void testLoadAndCompute_Transpose() {
        LinearAlgebraEngine lae = new LinearAlgebraEngine(2);

        // 2x3 Matrix
        double[][] data = {
                {1, 2, 3},
                {4, 5, 6}
        };
        ComputationNode nodeA = createLeafNode(data);
        ComputationNode transNode = createUnaryOpNode(ComputationNodeType.TRANSPOSE, nodeA);

        lae.loadAndCompute(transNode);

        double[][] res = transNode.getMatrix();

        // Should become 3x2
        assertEquals(3, res.length);
        assertEquals(2, res[0].length);

        // Check swap: (0,1) became (1,0)
        assertEquals(2.0, res[1][0]);
        // Check swap: (1,2) became (2,1)
        assertEquals(6.0, res[2][1]);
    }


    @Test
    void testLoadAndCompute_Multiply() {
        LinearAlgebraEngine lae = new LinearAlgebraEngine(2);

        // 1x2 * 2x1 = 1x1
        ComputationNode nodeA = createLeafNode(new double[][]{{3, 4}});
        ComputationNode nodeB = createLeafNode(new double[][]{{2}, {1}});
        ComputationNode mulNode = createOpNode(ComputationNodeType.MULTIPLY, nodeA, nodeB);

        lae.loadAndCompute(mulNode);

        double[][] res = mulNode.getMatrix();
        assertEquals(10.0, res[0][0]); // (3*2) + (4*1) = 10
    }


    @Test
    void testDimension_AddMismatch() {
        LinearAlgebraEngine lae = new LinearAlgebraEngine(2);
        ComputationNode n1 = createLeafNode(new double[][]{{1}});
        ComputationNode n2 = createLeafNode(new double[][]{{1, 2}}); // Diff size
        ComputationNode add = createOpNode(ComputationNodeType.ADD, n1, n2);

        Exception e = assertThrows(IllegalArgumentException.class, () -> lae.loadAndCompute(add));
        assertEquals("Illegal operation: dimensions mismatch", e.getMessage());
    }

    @Test
    void testDimension_MultMismatch() {
        LinearAlgebraEngine lae = new LinearAlgebraEngine(2);
        // A is 1x2 (Cols=2)
        ComputationNode n1 = createLeafNode(new double[][]{{1, 2}});
        // B is 3x1 (Rows=3) -> Mismatch!
        ComputationNode n2 = createLeafNode(new double[][]{{1}, {1}, {1}});
        ComputationNode mul = createOpNode(ComputationNodeType.MULTIPLY, n1, n2);

        Exception e = assertThrows(IllegalArgumentException.class, () -> lae.loadAndCompute(mul));
        assertEquals("Illegal operation: dimensions mismatch", e.getMessage());
    }

    @Test
    void testValidation_BinaryOp_MissingOperand() {
        LinearAlgebraEngine lae = new LinearAlgebraEngine(1);
        ComputationNode nodeA = createLeafNode(new double[][]{{1}});

        // ADD requires 2 children, we provide 1
        ComputationNode addNode = createUnaryOpNode(ComputationNodeType.ADD, nodeA);

        Exception e = assertThrows(IllegalArgumentException.class, () -> lae.loadAndCompute(addNode));
        assertEquals("Illegal operation: binary operator expects at least two operands", e.getMessage());
    }

    @Test
    void testValidation_UnaryOp_TooManyOperands() {
        LinearAlgebraEngine lae = new LinearAlgebraEngine(1);
        ComputationNode nodeA = createLeafNode(new double[][]{{1}});
        ComputationNode nodeB = createLeafNode(new double[][]{{2}});

        // NEGATE requires 1 child, we provide 2
        ComputationNode negNode = createOpNode(ComputationNodeType.NEGATE, nodeA, nodeB);

        Exception e = assertThrows(IllegalArgumentException.class, () -> lae.loadAndCompute(negNode));
        assertEquals("Illegal operation: unary operator expects exactly one operand", e.getMessage());
    }

}
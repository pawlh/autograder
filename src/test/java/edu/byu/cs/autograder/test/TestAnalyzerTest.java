package edu.byu.cs.autograder.test;

import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.model.TestNode;
import edu.byu.cs.util.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TestAnalyzerTest {

    private Set<String> extraCreditTests;

    @BeforeEach
    void setup() {
        extraCreditTests = new HashSet<>();
    }

    @Test
    @DisplayName("All tests pass")
    void parse__all_tests_pass() throws GradingException, IOException {
        String testsPassingInput =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <testsuite name="JUnit Jupiter" tests="2" skipped="0" failures="0" errors="0" time="0.025" hostname="acbcd3b36962" timestamp="2024-05-30T20:18:29">
                <testcase name="pawnMiddleOfBoardWhite()" classname="passoff.chess.piece.PawnMoveTests" time="0">
                <system-out><![CDATA[
                unique-id: [engine:junit-jupiter]/[class:passoff.chess.piece.PawnMoveTests]/[method:pawnMiddleOfBoardWhite()]
                display-name: pawnMiddleOfBoardWhite()
                ]]></system-out>
                </testcase>
                <testcase name="edgePromotionBlack()" classname="passoff.chess.piece.PawnMoveTests" time="0">
                <system-out><![CDATA[
                unique-id: [engine:junit-jupiter]/[class:passoff.chess.piece.PawnMoveTests]/[method:edgePromotionBlack()]
                display-name: edgePromotionBlack()
                ]]></system-out>
                </testcase>
                </testsuite>
                """;

        TestNode root = new TestAnalyzer().parse(xmlFromString(testsPassingInput), extraCreditTests).root();

        assertTrue(root.getTestName().startsWith("JUnit Jupiter"));
        assertEquals(2, root.getChildren().size());

        for (TestNode child : root.getChildren().values()) {
            assertTrue(child.getPassed());
            assertNull(child.getErrorMessage());
        }
    }

    @Test
    @DisplayName("All tests fail")
    void parse__all_tests_fail() throws GradingException, IOException {
        String testsFailingInput =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <testsuite name="JUnit Jupiter" tests="2" skipped="0" failures="0" errors="2" time="0.079" hostname="acbcd3b36962" timestamp="2024-05-30T20:55:26">
                <testcase name="pawnCaptureBlack()" classname="passoff.chess.piece.PawnMoveTests" time="0">
                <error message="Not implemented" type="java.lang.RuntimeException"><![CDATA[java.lang.RuntimeException: Not implemented
                    at chess.ChessBoard.addPiece(ChessBoard.java:22)
                    at passoffTests.TestFactory.loadBoard(TestFactory.java:104)
                    at passoffTests.TestFactory.validateMoves(TestFactory.java:70)
                    at passoffTests.chessTests.chessPieceTests.PawnMoveTests.pawnCaptureBlack(PawnMoveTests.java:227)
                    at java.base/java.lang.reflect.Method.invoke(Method.java:580)
                    at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
                    at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
                ]]></error>
                <system-out><![CDATA[
                unique-id: [engine:junit-jupiter]/[class:passoff.chess.piece.PawnMoveTests]/[method:pawnCaptureBlack()]
                display-name: pawnCaptureBlack()
                ]]></system-out>
                </testcase>
                <testcase name="pawnCaptureWhite()" classname="passoff.chess.piece.PawnMoveTests" time="0">
                <error message="Not implemented" type="java.lang.RuntimeException"><![CDATA[java.lang.RuntimeException: Not implemented
                    at chess.ChessBoard.addPiece(ChessBoard.java:22)
                    at passoffTests.TestFactory.loadBoard(TestFactory.java:104)
                    at passoffTests.TestFactory.validateMoves(TestFactory.java:70)
                    at passoffTests.chessTests.chessPieceTests.PawnMoveTests.pawnCaptureWhite(PawnMoveTests.java:210)
                    at java.base/java.lang.reflect.Method.invoke(Method.java:580)
                    at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
                    at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
                ]]></error>
                <system-out><![CDATA[
                unique-id: [engine:junit-jupiter]/[class:passoff.chess.piece.PawnMoveTests]/[method:pawnCaptureWhite()]
                display-name: pawnCaptureWhite()
                ]]></system-out>
                </testcase>
                </testsuite>
                """;

        TestNode root = new TestAnalyzer().parse(xmlFromString(testsFailingInput), extraCreditTests).root();

        assertTrue(root.getTestName().startsWith("JUnit Jupiter"));
        assertEquals(2, root.getChildren().size());

        for (TestNode child : root.getChildren().values()) {
            assertFalse(child.getPassed());
            assertNotNull(child.getErrorMessage());
        }
    }

    @Test
    @DisplayName("Some tests pass, some fail")
    void parse__some_pass_some_fail() throws GradingException, IOException {
        String testsPassingInput =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <testsuite name="JUnit Jupiter" tests="2" skipped="0" failures="0" errors="0" time="0.025" hostname="acbcd3b36962" timestamp="2024-05-30T20:18:29">
                <testcase name="pawnMiddleOfBoardWhite()" classname="passoff.chess.piece.PawnMoveTests" time="0">
                <system-out><![CDATA[
                unique-id: [engine:junit-jupiter]/[class:passoff.chess.piece.PawnMoveTests]/[method:pawnMiddleOfBoardWhite()]
                display-name: pawnMiddleOfBoardWhite()
                ]]></system-out>
                </testcase>
                <testcase name="pawnCaptureBlack()" classname="passoff.chess.piece.PawnMoveTests" time="0">
                <error message="Not implemented" type="java.lang.RuntimeException"><![CDATA[java.lang.RuntimeException: Not implemented
                    at chess.ChessBoard.addPiece(ChessBoard.java:22)
                    at passoffTests.TestFactory.loadBoard(TestFactory.java:104)
                    at passoffTests.TestFactory.validateMoves(TestFactory.java:70)
                    at passoffTests.chessTests.chessPieceTests.PawnMoveTests.pawnCaptureBlack(PawnMoveTests.java:227)
                    at java.base/java.lang.reflect.Method.invoke(Method.java:580)
                    at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
                    at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
                ]]></error>
                <system-out><![CDATA[
                unique-id: [engine:junit-jupiter]/[class:passoff.chess.piece.PawnMoveTests]/[method:pawnCaptureBlack()]
                display-name: pawnCaptureBlack()
                ]]></system-out>
                </testcase>
                <testcase name="edgePromotionBlack()" classname="passoff.chess.piece.PawnMoveTests" time="0">
                <system-out><![CDATA[
                unique-id: [engine:junit-jupiter]/[class:passoff.chess.piece.PawnMoveTests]/[method:edgePromotionBlack()]
                display-name: edgePromotionBlack()
                ]]></system-out>
                </testcase>
                <testcase name="rookBlocked()" classname="passoff.chess.piece.RookMoveTests" time="0">
                <error message="Not implemented" type="java.lang.RuntimeException"><![CDATA[java.lang.RuntimeException: Not implemented
                    at chess.ChessBoard.addPiece(ChessBoard.java:22)
                    at passoffTests.TestFactory.loadBoard(TestFactory.java:104)
                    at passoffTests.TestFactory.validateMoves(TestFactory.java:70)
                    at passoffTests.chessTests.chessPieceTests.RookMoveTests.rookBlocked(RookMoveTests.java:57)
                    at java.base/java.lang.reflect.Method.invoke(Method.java:580)
                    at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
                    at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
                ]]></error>
                <system-out><![CDATA[
                unique-id: [engine:junit-jupiter]/[class:passoff.chess.piece.PawnMoveTests]/[method:pawnCaptureWhite()]
                display-name: pawnCaptureWhite()
                ]]></system-out>
                </testcase>
                <testcase name="castlingBlockedByEnemy()" classname="passoff.chess.extracredit.CastlingTests" time="0">
                <error message="Not implemented" type="java.lang.RuntimeException"><![CDATA[java.lang.RuntimeException: Not implemented
                        at chess.ChessBoard.addPiece(ChessBoard.java:22)
                        at passoffTests.chessTests.chessExtraCredit.CastlingTests.castlingBlockedByEnemy(CastlingTests.java:306)
                        at java.base/java.lang.reflect.Method.invoke(Method.java:580)
                        at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
                        at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
                ]]></error>
                <system-out><![CDATA[
                unique-id: [engine:junit-jupiter]/[passoff.chess.extracredit.CastlingTests]/[method:castlingBlockedByEnemy()]
                display-name: Cannot Castle in Check
                ]]></system-out>
                </testcase>
                </testsuite>
                """;

        TestNode root = new TestAnalyzer().parse(xmlFromString(testsPassingInput), extraCreditTests).root();

        assertTrue(root.getTestName().startsWith("JUnit Jupiter"));
        assertEquals(3, root.getChildren().get("piece").getChildren().get("PawnMoveTests").getChildren().size());
        assertEquals(1, root.getChildren().get("piece").getChildren().get("RookMoveTests").getChildren().size());
        assertEquals(1, root.getChildren().get("extracredit").getChildren().size());

        for (TestNode child : root.getChildren().get("piece").getChildren().get("PawnMoveTests").getChildren().values()) {
            switch (child.getTestName()) {
                case "pawnMiddleOfBoardWhite()", "edgePromotionBlack()" -> {
                    assertTrue(child.getPassed());
                    assertNull(child.getErrorMessage());
                }
                case "pawnCaptureBlack()" -> {
                    assertFalse(child.getPassed());
                    assertNotNull(child.getErrorMessage());
                }
                default -> fail("Unexpected test name: " + child.getTestName());
            }
        }
    }

//    @Test
//    @DisplayName("Counts are correct")
//    void TestNode__counts_are_correct() throws GradingException {
//        String testsPassingInput =
//                """
//                JUnit Jupiter > PawnMoveTests > pawnMiddleOfBoardWhite() :: STARTED
//                JUnit Jupiter > PawnMoveTests > pawnMiddleOfBoardWhite() :: SUCCESSFUL
//                JUnit Jupiter > PawnMoveTests > edgePromotionBlack() :: STARTED
//                JUnit Jupiter > PawnMoveTests > edgePromotionBlack() :: SUCCESSFUL
//                JUnit Jupiter > RookMoveTests > rookBlocked() :: STARTED
//                JUnit Jupiter > RookMoveTests > rookBlocked() :: FAILED
//                    java.lang.RuntimeException: Not implemented
//                        at chess.ChessBoard.addPiece(ChessBoard.java:22)
//                        at passoffTests.TestFactory.loadBoard(TestFactory.java:104)
//                        at passoffTests.TestFactory.validateMoves(TestFactory.java:70)
//                        at passoffTests.chessTests.chessPieceTests.RookMoveTests.rookBlocked(RookMoveTests.java:57)
//                        at java.base/java.lang.reflect.Method.invoke(Method.java:580)
//                        at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
//                        at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
//                """;
//
//        TestNode root = new TestAnalyzer().parse(testsPassingInput.split("\n"), extraCreditTests, null).root();
//
//        assertEquals(2, root.getNumTestsPassed());
//        assertEquals(1, root.getNumTestsFailed());
//
//        assertEquals(2, root.getChildren().get("PawnMoveTests").getNumTestsPassed());
//        assertEquals(0, root.getChildren().get("PawnMoveTests").getNumTestsFailed());
//
//        assertEquals(0, root.getChildren().get("RookMoveTests").getNumTestsPassed());
//        assertEquals(1, root.getChildren().get("RookMoveTests").getNumTestsFailed());
//    }

//    @Test
//    @DisplayName("Extra credit all successful")
//    void parse__ec_all_success() throws GradingException {
//        String testsPassingInput =
//                """
//                JUnit Jupiter > CastlingTests > Cannot Castle in Check :: STARTED
//                JUnit Jupiter > CastlingTests > Cannot Castle in Check :: SUCCESSFUL
//                JUnit Jupiter > CastlingTests > Black Team Castle :: STARTED
//                JUnit Jupiter > CastlingTests > Black Team Castle :: SUCCESSFUL
//                JUnit Jupiter > CastlingTests > White Team Castle :: STARTED
//                JUnit Jupiter > CastlingTests > White Team Castle :: SUCCESSFUL
//                JUnit Jupiter > EnPassantTests > Black En Passant Left :: STARTED
//                JUnit Jupiter > EnPassantTests > Black En Passant Left :: SUCCESSFUL
//                JUnit Jupiter > EnPassantTests > White En Passant Right :: STARTED
//                JUnit Jupiter > EnPassantTests > White En Passant Right :: SUCCESSFUL
//                JUnit Jupiter > EnPassantTests > White En Passant Left :: STARTED
//                JUnit Jupiter > EnPassantTests > White En Passant Left :: SUCCESSFUL
//                JUnit Jupiter > ChessGameTests > Black in Check :: STARTED
//                JUnit Jupiter > ChessGameTests > Black in Check :: SUCCESSFUL
//                JUnit Jupiter > ChessGameTests > White in Checkmate :: STARTED
//                JUnit Jupiter > ChessGameTests > White in Checkmate :: SUCCESSFUL
//                JUnit Jupiter > ChessGameTests > Normal Make Move :: STARTED
//                JUnit Jupiter > ChessGameTests > Normal Make Move :: SUCCESSFUL
//                JUnit Jupiter > ChessGameTests > White in Check :: STARTED
//                JUnit Jupiter > ChessGameTests > White in Check :: SUCCESSFUL
//                """;
//        extraCreditTests.add("CastlingTests");
//        extraCreditTests.add("EnPassantTests");
//
//        TestNode root = new TestAnalyzer().parse(testsPassingInput.split("\n"), extraCreditTests, null).root();
//        assertEquals("JUnit Jupiter", root.getTestName());
//        assertEquals(4, root.getNumTestsPassed());
//        assertEquals(6, root.getNumExtraCreditPassed());
//        assertEquals("CastlingTests", root.getChildren().get("CastlingTests").getEcCategory());
//        assertEquals("EnPassantTests", root.getChildren().get("EnPassantTests").getEcCategory());
//        assertNull(root.getChildren().get("ChessGameTests").getEcCategory());
//    }

    private File xmlFromString(String xml) throws IOException {
        File file = File.createTempFile("tmp-" + System.currentTimeMillis(), "xml");
        FileUtils.writeStringToFile(xml, file);
        return file;
    }
}

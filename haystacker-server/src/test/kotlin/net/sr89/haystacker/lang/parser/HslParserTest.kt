package net.sr89.haystacker.lang.parser

import net.sr89.haystacker.lang.ast.HslAndClause
import net.sr89.haystacker.lang.ast.HslDataSize
import net.sr89.haystacker.lang.ast.HslDate
import net.sr89.haystacker.lang.ast.HslInstant
import net.sr89.haystacker.lang.ast.HslNodeClause
import net.sr89.haystacker.lang.ast.HslOrClause
import net.sr89.haystacker.lang.ast.HslString
import net.sr89.haystacker.lang.ast.Operator
import net.sr89.haystacker.lang.ast.Symbol
import net.sr89.haystacker.lang.exception.InvalidHslGrammarException
import net.sr89.haystacker.test.common.having
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.springframework.util.unit.DataSize
import java.time.Instant
import java.time.LocalDate

class HslParserTest {
    private val date = "2020-01-01"
    private val dateTime = "2020-01-17T10:15:50Z"
    private val dateTimeWithOffset = "2020-01-17T10:15:30+04:00"

    private val parser = HslParser()

    private fun <T> HslNodeClause.isNodeClause(symbol: Symbol, operator: Operator, value: T) {
        assertEquals(symbol, this.symbol)
        assertEquals(operator, this.operator)
        assertEquals(value, this.value)
    }

    @Test
    fun nameEqualToClauseWithSpacesInFilename() {
        val query = parser.parse("name = \"name with spaces.txt\"")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.NAME, Operator.EQUALS, HslString("name with spaces.txt"))
            }
    }

    @Test
    fun nameEqualToClause() {
        val query = parser.parse("name = \"file.txt\"")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.NAME, Operator.EQUALS, HslString("file.txt"))
            }
    }

    @Test
    fun nameEqualToClauseNoQuotes() {
        val query = parser.parse("name = file.txt")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.NAME, Operator.EQUALS, HslString("file.txt"))
            }
    }

    @Test
    fun sizeAssumesBytesIfNoUnitSpecified() {
        val query = parser.parse("size > 23")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.SIZE, Operator.GREATER, HslDataSize(DataSize.ofBytes(23)))
            }
    }

    @Test
    fun sizeGreaterThanBClause() {
        val query = parser.parse("size > 23b")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.SIZE, Operator.GREATER, HslDataSize(DataSize.ofBytes(23)))
            }
    }

    @Test
    fun sizeGreaterThanKBClause() {
        val query = parser.parse("size > 23kb")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.SIZE, Operator.GREATER, HslDataSize(DataSize.ofKilobytes(23)))
            }
    }

    @Test
    fun sizeGreaterThanMBClause() {
        val query = parser.parse("size > 23mb")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.SIZE, Operator.GREATER, HslDataSize(DataSize.ofMegabytes(23)))
            }
    }

    @Test
    fun sizeGreaterThanGBClause() {
        val query = parser.parse("size > 23gb")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.SIZE, Operator.GREATER, HslDataSize(DataSize.ofGigabytes(23)))
            }
    }

    @Test
    fun sizeGreaterThanTBClause() {
        val query = parser.parse("size > 23tb")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.SIZE, Operator.GREATER, HslDataSize(DataSize.ofTerabytes(23)))
            }
    }

    @Test
    fun lastModifiedGreaterThanDate() {
        val query = parser.parse("last_modified > '$date'")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.LAST_MODIFIED, Operator.GREATER, HslDate(LocalDate.parse(date)))
            }
    }

    @Test
    fun lastModifiedGreaterThanDateTime() {
        val query = parser.parse("last_modified > '$dateTime'")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.LAST_MODIFIED, Operator.GREATER, HslInstant(Instant.ofEpochSecond(1579256150L)))
            }
    }

    @Test
    fun lastModifiedGreaterThanDateTimeWithOffset() {
        val query = parser.parse("last_modified > '$dateTimeWithOffset'")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.LAST_MODIFIED, Operator.GREATER, HslInstant(Instant.ofEpochSecond(1579241730L)))
            }
    }

    @Test
    fun lastModifiedGreaterOrEqualThanDate() {
        val query = parser.parse("last_modified >= '$date'")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.LAST_MODIFIED, Operator.GREATER_OR_EQUAL, HslDate(LocalDate.parse(date)))
            }
    }

    @Test
    fun lastModifiedLessOrEqualThanDate() {
        val query = parser.parse("last_modified <= '$date'")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.LAST_MODIFIED, Operator.LESS_OR_EQUAL, HslDate(LocalDate.parse(date)))
            }
    }

    @Test
    fun createdLessThanDate() {
        val query = parser.parse("created < '$date'")

        having(query)
            .ofType(HslNodeClause::class)
            .then {
                it.isNodeClause(Symbol.CREATED, Operator.LESS, HslDate(LocalDate.parse(date)))
            }
    }

    @Test
    fun andClause() {
        val query = parser.parse("created < '$date' AND name = \"file.txt\"")

        having(query)
            .ofType(HslAndClause::class)
            .then {
                having(it.left)
                    .ofType(HslNodeClause::class)
                    .then {left ->
                        left.isNodeClause(Symbol.CREATED, Operator.LESS, HslDate(LocalDate.parse(date)))
                    }

                having(it.right)
                    .ofType(HslNodeClause::class)
                    .then {right ->
                        right.isNodeClause(Symbol.NAME, Operator.EQUALS, HslString("file.txt"))
                    }
            }
    }

    @Test
    fun orClause() {
        val query = parser.parse("created < '$date' OR name = \"file.txt\"")

        having(query)
            .ofType(HslOrClause::class)
            .then {
                having(it.left)
                    .ofType(HslNodeClause::class)
                    .then {left ->
                        left.isNodeClause(Symbol.CREATED, Operator.LESS, HslDate(LocalDate.parse(date)))
                    }

                having(it.right)
                    .ofType(HslNodeClause::class)
                    .then {right ->
                        right.isNodeClause(Symbol.NAME, Operator.EQUALS, HslString("file.txt"))
                    }
            }
    }

    @Test
    fun andHasPrecedenceOverOr() {
        val query = parser.parse("created < '$date' AND created < '$date' OR name = \"file.txt\"")

        having(query)
            .ofType(HslOrClause::class)
            .then {
                having(it.left)
                    .ofType(HslAndClause::class)
                    .then {
                    }

                having(it.right)
                    .ofType(HslNodeClause::class)
                    .then {right ->
                        right.isNodeClause(Symbol.NAME, Operator.EQUALS, HslString("file.txt"))
                    }
            }
    }

    @Test
    fun parensForcePrecedence() {
        val query = parser.parse("created < '$date' AND (created < '$date' OR name = \"file.txt\")")

        having(query)
            .ofType(HslAndClause::class)
            .then {
                having(it.left)
                    .ofType(HslNodeClause::class)
                    .then {left ->
                        left.isNodeClause(Symbol.CREATED, Operator.LESS, HslDate(LocalDate.parse(date)))
                    }

                having(it.right)
                    .ofType(HslOrClause::class)
                    .then {
                    }
            }
    }

    @Test
    fun spacesAreIgnored() {
        val query = parser.parse("  created  <   '$date'     AND    (  created   <   '$date'   OR  name   =   \"file.txt\"  ) ")

        having(query)
            .ofType(HslAndClause::class)
            .then {
                having(it.left)
                    .ofType(HslNodeClause::class)
                    .then {left ->
                        left.isNodeClause(Symbol.CREATED, Operator.LESS, HslDate(LocalDate.parse(date)))
                    }

                having(it.right)
                    .ofType(HslOrClause::class)
                    .then {
                    }
            }
    }

    @Test
    internal fun brokenGrammarThrowsSensibleException() {
        try {
            parser.parse("this is a broken query")
            fail<String>("Expected parsing to fail for a broken HSL query")
        } catch (e: InvalidHslGrammarException) {

        }
    }
}
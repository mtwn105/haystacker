package net.sr89.haystacker.index

import net.sr89.haystacker.test.common.timeAction
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.LongPoint
import org.apache.lucene.document.TextField
import org.apache.lucene.index.Term
import org.apache.lucene.search.TermQuery
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertTrue

internal class IndexManagerPerformanceTest {

    @Test @Disabled
    internal fun indexManyDocuments() {
        val tempDir = Files.createTempDirectory("tdir").toFile()
        val manager = IndexManager.forPath(tempDir.toString())
        val directoryDepth = 100
        val filesPerDirectory = 10000

        try {
            var fileToFind: String? = null
            var pathToFind: Path? = null

            manager.createNewIndex().use {
                for (i in 0..directoryDepth) {
                    val directory = randomPath(10)

                    if (i % 10 == 0) {
                        println("Creating document ${i * filesPerDirectory} out of ${directoryDepth * filesPerDirectory}")
                    }

                    for (j in 0..filesPerDirectory) {
                        val fileName = randomString(10)
                        val path = Path.of(directory.toString(), fileName)
                        val document = testDocument(path.toString(), (i * j).toLong())
                        val documentId = Term("path", path.toString())
                        it.updateDocument(documentId, document)

                        if (i * j == 40) {
                            fileToFind = fileName
                            pathToFind = path
                        }
                    }
                }
            }

            timeAction({findDocumentsByPath(manager, fileToFind!!, pathToFind!!)}, "Find documents by path")
            timeAction({findDocumentsByLongRange(manager)}, "Find documents by long range")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun findDocumentsByPath(manager: IndexManager, fileToFind: String, pathToFind: Path) {
        val foundByFileName = manager.searchIndex(TermQuery(Term("path", fileToFind.toLowerCase())))
        val foundByPathPart = manager.searchIndex(TermQuery(Term("path", pathToFind.parent.parent.parent.fileName.toString().toLowerCase())))

        assertTrue(foundByFileName.totalHits.value >= 1L, "At least one file should be found by filename")
        assertTrue(foundByPathPart.totalHits.value >= 1L, "At least one file should be found by path part")
    }

    private fun findDocumentsByLongRange(manager: IndexManager) {
        val foundByRange = manager.searchIndex(LongPoint.newRangeQuery("modified", 1010, 40000))

        assertTrue(foundByRange.totalHits.value >= 1L, "At least one file should be found by range")
    }

    private fun testDocument(path: String, number: Long): Document {
        val doc = Document()

        doc.add(TextField("path", path, Field.Store.YES))
        doc.add(LongPoint("modified", number))

        return doc
    }

    fun randomPath(length: Int): Path {
        val pathParts = listOf("part", "directory", "orange", "random", "icecream", "files", "mystuff", "things", "items", "objects")

        return (1..length)
            .map { pathParts.random() }
            .fold(Paths.get("")) { currPath, next ->
                Path.of(currPath.toString(), next)
            }
    }

    fun randomString(length: Int): String {
        val charset = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { charset.random() }
            .joinToString("")
    }
}